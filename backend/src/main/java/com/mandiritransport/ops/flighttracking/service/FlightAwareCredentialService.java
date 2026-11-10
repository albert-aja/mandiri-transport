package com.mandiritransport.ops.flighttracking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.auth.SecurityUtils;
import com.mandiritransport.ops.booking.entity.Booking;
import com.mandiritransport.ops.booking.mapper.BookingMapper;
import com.mandiritransport.ops.common.audit.AuditService;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.CredentialSummaryDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.DiscoveredFieldItem;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.ExecutePollRequest;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.ExtractTokenRequest;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.FlightDataValidationReportDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.NormalizedFlightSummary;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.PollDiffComparisonDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.PollExecutionResultDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.TokenExtractionSummaryDto;
import com.mandiritransport.ops.flighttracking.entity.FlightTrackingCredential;
import com.mandiritransport.ops.flighttracking.entity.FlightTrackingPollLog;
import com.mandiritransport.ops.flighttracking.mapper.FlightTrackingCredentialMapper;
import com.mandiritransport.ops.flighttracking.mapper.FlightTrackingPollLogMapper;
import com.mandiritransport.ops.flighttracking.provider.FlightTrackingProvider.ExtractedTokenPayload;
import com.mandiritransport.ops.flighttracking.provider.FlightTrackingProvider.NormalizedPollPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages the full lifecycle of encrypted FlightAware tracking credentials and research poll logs (Sections 5, 6, 8).
 *
 * Key guarantees:
 * 1. Tokens are encrypted with AES-256-GCM before database persistence and never returned in DTOs or logs.
 * 2. Never overwrites a working (`ACTIVE`) credential with a newly extracted token until the replacement
 *    has been validated against the polling endpoint (HTTP 200 + valid tracking JSON + matching flight identity
 *    + valid operational data).
 * 3. Prevents duplicate `ACTIVE` credentials for the same `(provider, flightNumber, flightDate)` by superseding
 *    prior active credentials (`SUPERSEDED`) only after replacement validation succeeds.
 * 4. Enforces conservative polling intervals and avoids duplicate poll log entries on identical immediate retries.
 */
@Service
public class FlightAwareCredentialService {

    private static final Pattern URL_FLIGHT_CODE_PATTERN = Pattern.compile(
            "/flight/([A-Za-z0-9]{2,8})(?:/|$|\\?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> INDONESIAN_AIRLINE_IATA_TO_ICAO = Map.of(
            "GA", "GIA",
            "JT", "LNI",
            "ID", "BTK",
            "QG", "CTV",
            "IU", "SJV",
            "IW", "WON",
            "QZ", "AWQ",
            "AK", "AXM",
            "SQ", "SIA",
            "MH", "MAS"
    );

    private final FlightAwareProviderAdapter providerAdapter;
    private final FlightTokenCryptoService cryptoService;
    private final FlightAwareDiffService diffService;
    private final FlightDataValidationReportService validationReportService;
    private final FlightTrackingCredentialMapper credentialMapper;
    private final FlightTrackingPollLogMapper pollLogMapper;
    private final BookingMapper bookingMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final int minPollIntervalSeconds;
    private final int pollLogRetentionDays;

    public FlightAwareCredentialService(
            FlightAwareProviderAdapter providerAdapter,
            FlightTokenCryptoService cryptoService,
            FlightAwareDiffService diffService,
            FlightDataValidationReportService validationReportService,
            FlightTrackingCredentialMapper credentialMapper,
            FlightTrackingPollLogMapper pollLogMapper,
            BookingMapper bookingMapper,
            AuditService auditService,
            ObjectMapper objectMapper,
            @Value("${app.flight-tracking.min-poll-interval-seconds:15}") int minPollIntervalSeconds,
            @Value("${app.flight-tracking.poll-log-retention-days:30}") int pollLogRetentionDays) {
        this.providerAdapter = providerAdapter;
        this.cryptoService = cryptoService;
        this.diffService = diffService;
        this.validationReportService = validationReportService;
        this.credentialMapper = credentialMapper;
        this.pollLogMapper = pollLogMapper;
        this.bookingMapper = bookingMapper;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.minPollIntervalSeconds = Math.max(0, minPollIntervalSeconds);
        this.pollLogRetentionDays = Math.max(1, pollLogRetentionDays);
    }

    @Transactional
    public TokenExtractionSummaryDto extractToken(ExtractTokenRequest req) {
        ExtractedTokenPayload extracted = providerAdapter.extractTrackingMetadata(req.trackingPageUrl());
        boolean tokenPresent = extracted.rawToken() != null && !extracted.rawToken().isBlank();
        boolean userTokenPresent = extracted.rawUserToken() != null && !extracted.rawUserToken().isBlank();
        String fingerprint = tokenPresent ? cryptoService.fingerprint(extracted.rawToken()) : "none";

        CredentialSummaryDto persistedDto = null;
        boolean shouldPersist = req.persistCredential() == null || Boolean.TRUE.equals(req.persistCredential());

        if (extracted.success() && tokenPresent && shouldPersist) {
            Booking linkedBooking = null;
            if (req.flightId() != null && !req.flightId().isBlank()) {
                linkedBooking = bookingMapper.selectById(req.flightId().trim());
                if (linkedBooking == null) {
                    throw BusinessException.notFound("Data booking penerbangan tidak ditemukan: " + req.flightId());
                }
            }

            String resolvedFlightNumber = resolveFlightNumber(req.flightNumber(), linkedBooking, req.trackingPageUrl());
            LocalDate resolvedFlightDate = resolveFlightDate(req.flightDate(), linkedBooking);

            FlightTrackingCredential cred = persistExtractedCredentialInternal(
                    extracted,
                    req.flightId(),
                    req.tripId(),
                    resolvedFlightNumber,
                    resolvedFlightDate,
                    "Diekstrak dari halaman pelacakan; menunggu validasi polling."
            );

            if (Boolean.TRUE.equals(req.validateImmediately())) {
                validateCredentialInternal(cred, true, "INITIAL_VALIDATION");
            }

            FlightTrackingCredential reloaded = credentialMapper.selectById(cred.getId());
            persistedDto = toCredentialSummaryDto(reloaded, linkedBooking != null ? linkedBooking.getBookingCode() : null);
        }

        return new TokenExtractionSummaryDto(
                extracted.success(),
                extracted.status(),
                extracted.httpStatus(),
                req.trackingPageUrl(),
                tokenPresent,
                fingerprint,
                extracted.pollingIntervalSeconds(),
                extracted.singleFlight(),
                userTokenPresent,
                extracted.additionalDiscoveredKeys(),
                extracted.extractedAt(),
                extracted.diagnosticMessage(),
                persistedDto
        );
    }

    @Transactional
    public CredentialSummaryDto replaceCredential(String existingCredentialId, String replacementTrackingPageUrl) {
        FlightTrackingCredential existing = requireCredential(existingCredentialId);
        ExtractedTokenPayload extracted = providerAdapter.extractTrackingMetadata(replacementTrackingPageUrl);
        if (!extracted.success() || extracted.rawToken() == null || extracted.rawToken().isBlank()) {
            throw BusinessException.badRequest(
                    "REPLACEMENT_EXTRACTION_FAILED",
                    "Gagal mengekstrak token pengganti: " + extracted.diagnosticMessage()
                            + ". Kredensial lama tetap dipertahankan (" + existing.getTokenStatus() + ")."
            );
        }

        // Persist candidate replacement in UNKNOWN state first — never overwrite working credential before validation!
        FlightTrackingCredential candidate = persistExtractedCredentialInternal(
                extracted,
                existing.getFlightId(),
                existing.getTripId(),
                existing.getFlightNumber(),
                existing.getFlightDate(),
                "Kandidat pengganti untuk kredensial " + existing.getId()
        );

        PollExecutionResultDto validationPoll = validateCredentialInternal(candidate, true, "REPLACEMENT_VALIDATION");
        FlightTrackingCredential updatedCandidate = requireCredential(candidate.getId());

        if (!"ACTIVE".equals(updatedCandidate.getTokenStatus())) {
            // Candidate failed validation! Keep existing working credential intact.
            auditService.log("FLIGHT_TRACKING_CREDENTIAL", existing.getId(), "REPLACEMENT_REJECTED",
                    "Penggantian kredensial ditolak karena kandidat gagal validasi",
                    Map.of("existingStatus", existing.getTokenStatus()),
                    Map.of("candidateId", updatedCandidate.getId(), "reason", validationPoll.validationMessage()));
            return toCredentialSummaryDto(updatedCandidate, null);
        }

        // Candidate passed full validation -> supersede existing credential if not already superseded
        FlightTrackingCredential refreshedExisting = requireCredential(existing.getId());
        if ("ACTIVE".equals(refreshedExisting.getTokenStatus())) {
            refreshedExisting.setTokenStatus("SUPERSEDED");
            refreshedExisting.setValidationNotes("Digantikan oleh kredensial tervalidasi " + updatedCandidate.getId());
            refreshedExisting.setUpdatedBy(currentActorId());
            refreshedExisting.setUpdatedAt(OffsetDateTime.now());
            credentialMapper.updateById(refreshedExisting);
        }

        auditService.log("FLIGHT_TRACKING_CREDENTIAL", updatedCandidate.getId(), "CREDENTIAL_REPLACED",
                "Kredensial digantikan dengan token baru yang tervalidasi",
                Map.of("supersededCredentialId", existing.getId()),
                Map.of("newStatus", updatedCandidate.getTokenStatus()));
        return toCredentialSummaryDto(updatedCandidate, null);
    }

    @Transactional
    public PollExecutionResultDto executePollAndValidate(String credentialId, ExecutePollRequest req) {
        FlightTrackingCredential cred = requireCredential(credentialId);
        if ("DISABLED".equals(cred.getTokenStatus())) {
            throw BusinessException.badRequest("CREDENTIAL_DISABLED", "Kredensial ini telah dinonaktifkan oleh administrator.");
        }
        boolean bypassMinInterval = req != null && Boolean.TRUE.equals(req.forceBypassMinInterval());
        String experimentTag = req != null ? req.experimentTag() : null;
        return validateCredentialInternal(cred, bypassMinInterval, experimentTag);
    }

    @Transactional
    public CredentialSummaryDto revalidateCredential(String credentialId) {
        FlightTrackingCredential cred = requireCredential(credentialId);
        validateCredentialInternal(cred, true, "MANUAL_REVALIDATION");
        return toCredentialSummaryDto(requireCredential(credentialId), null);
    }

    @Transactional
    public CredentialSummaryDto disableCredential(String credentialId) {
        FlightTrackingCredential cred = requireCredential(credentialId);
        String prevStatus = cred.getTokenStatus();
        cred.setTokenStatus("DISABLED");
        cred.setValidationNotes("Dinonaktifkan secara manual oleh administrator.");
        cred.setUpdatedBy(currentActorId());
        cred.setUpdatedAt(OffsetDateTime.now());
        credentialMapper.updateById(cred);

        auditService.log("FLIGHT_TRACKING_CREDENTIAL", cred.getId(), "DISABLE_CREDENTIAL",
                "Kredensial dinonaktifkan secara manual",
                Map.of("previousStatus", prevStatus),
                Map.of("newStatus", "DISABLED"));
        return toCredentialSummaryDto(cred, null);
    }

    public List<CredentialSummaryDto> listCredentials(String flightNumber, String tokenStatus) {
        LambdaQueryWrapper<FlightTrackingCredential> query = new LambdaQueryWrapper<FlightTrackingCredential>()
                .orderByDesc(FlightTrackingCredential::getExtractedAt);
        if (flightNumber != null && !flightNumber.isBlank()) {
            query.eq(FlightTrackingCredential::getFlightNumber, normalizeFlightNumberCode(flightNumber));
        }
        if (tokenStatus != null && !tokenStatus.isBlank()) {
            query.eq(FlightTrackingCredential::getTokenStatus, tokenStatus.trim().toUpperCase(Locale.ROOT));
        }
        List<FlightTrackingCredential> list = credentialMapper.selectList(query);
        Map<String, String> bookingCodes = loadBookingCodeMap(list);
        return list.stream()
                .map(c -> toCredentialSummaryDto(c, bookingCodes.get(c.getFlightId())))
                .toList();
    }

    public List<CredentialSummaryDto> getCredentialHistoryForFlight(String flightNumber, LocalDate flightDate) {
        LambdaQueryWrapper<FlightTrackingCredential> query = new LambdaQueryWrapper<FlightTrackingCredential>()
                .eq(FlightTrackingCredential::getFlightNumber, normalizeFlightNumberCode(flightNumber))
                .orderByDesc(FlightTrackingCredential::getExtractedAt);
        if (flightDate != null) {
            query.eq(FlightTrackingCredential::getFlightDate, flightDate);
        }
        List<FlightTrackingCredential> list = credentialMapper.selectList(query);
        Map<String, String> bookingCodes = loadBookingCodeMap(list);
        return list.stream()
                .map(c -> toCredentialSummaryDto(c, bookingCodes.get(c.getFlightId())))
                .toList();
    }

    public List<PollExecutionResultDto> listPollLogs(String credentialId) {
        LambdaQueryWrapper<FlightTrackingPollLog> query = new LambdaQueryWrapper<FlightTrackingPollLog>()
                .orderByDesc(FlightTrackingPollLog::getRequestTimestamp);
        if (credentialId != null && !credentialId.isBlank()) {
            query.eq(FlightTrackingPollLog::getCredentialId, credentialId.trim());
        }
        query.last("LIMIT 100");
        FlightTrackingCredential cred = (credentialId != null && !credentialId.isBlank())
                ? credentialMapper.selectById(credentialId.trim())
                : null;
        String credStatus = cred != null ? cred.getTokenStatus() : null;
        String credNotes = cred != null ? cred.getValidationNotes() : null;

        return pollLogMapper.selectList(query).stream()
                .map(l -> toPollExecutionResultDto(l, credStatus, credNotes))
                .toList();
    }

    public PollDiffComparisonDto comparePollLogs(String olderPollLogId, String newerPollLogId) {
        FlightTrackingPollLog older = pollLogMapper.selectById(olderPollLogId);
        FlightTrackingPollLog newer = pollLogMapper.selectById(newerPollLogId);
        if (older == null || newer == null) {
            throw BusinessException.notFound("Log polling untuk perbandingan tidak ditemukan.");
        }
        return diffService.comparePollLogs(older, newer);
    }

    public PollDiffComparisonDto compareLatestTwoPollsForCredential(String credentialId) {
        List<FlightTrackingPollLog> recent = pollLogMapper.selectList(
                new LambdaQueryWrapper<FlightTrackingPollLog>()
                        .eq(FlightTrackingPollLog::getCredentialId, credentialId)
                        .orderByDesc(FlightTrackingPollLog::getRequestTimestamp)
                        .last("LIMIT 2")
        );
        if (recent.size() < 2) {
            throw BusinessException.badRequest(
                    "INSUFFICIENT_POLL_LOGS",
                    "Membutuhkan minimal 2 log polling pada kredensial ini untuk membandingkan perubahan."
            );
        }
        return diffService.comparePollLogs(recent.get(1), recent.get(0));
    }

    public FlightDataValidationReportDto generateValidationReport(String credentialId) {
        List<FlightTrackingCredential> creds;
        List<FlightTrackingPollLog> logs;
        if (credentialId != null && !credentialId.isBlank()) {
            FlightTrackingCredential cred = requireCredential(credentialId.trim());
            creds = List.of(cred);
            logs = pollLogMapper.selectList(
                    new LambdaQueryWrapper<FlightTrackingPollLog>()
                            .eq(FlightTrackingPollLog::getCredentialId, cred.getId())
                            .orderByAsc(FlightTrackingPollLog::getRequestTimestamp)
            );
        } else {
            creds = credentialMapper.selectList(
                    new LambdaQueryWrapper<FlightTrackingCredential>()
                            .orderByDesc(FlightTrackingCredential::getExtractedAt)
            );
            logs = pollLogMapper.selectList(
                    new LambdaQueryWrapper<FlightTrackingPollLog>()
                            .orderByAsc(FlightTrackingPollLog::getRequestTimestamp)
            );
        }
        return validationReportService.buildValidationReport(credentialId, creds, logs);
    }

    @Transactional
    public int purgeExpiredPollLogs() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(pollLogRetentionDays);
        return pollLogMapper.delete(
                new LambdaQueryWrapper<FlightTrackingPollLog>()
                        .lt(FlightTrackingPollLog::getCreatedAt, cutoff)
        );
    }

    // --- Internal Helpers ---

    private FlightTrackingCredential persistExtractedCredentialInternal(
            ExtractedTokenPayload extracted,
            String flightId,
            String tripId,
            String flightNumber,
            LocalDate flightDate,
            String initialNote) {

        String encryptedToken = cryptoService.encrypt(extracted.rawToken());
        String encryptedUserToken = extracted.rawUserToken() != null && !extracted.rawUserToken().isBlank()
                ? cryptoService.encrypt(extracted.rawUserToken())
                : null;
        String fingerprint = cryptoService.fingerprint(extracted.rawToken());

        OffsetDateTime now = OffsetDateTime.now();
        String actor = currentActorId();

        FlightTrackingCredential cred = new FlightTrackingCredential();
        cred.setId(UUID.randomUUID().toString());
        cred.setFlightId(flightId != null && !flightId.isBlank() ? flightId.trim() : null);
        cred.setTripId(tripId != null && !tripId.isBlank() ? tripId.trim() : null);
        cred.setProvider(providerAdapter.providerCode());
        cred.setFlightNumber(flightNumber);
        cred.setFlightDate(flightDate);
        cred.setTrackingTokenEncrypted(encryptedToken);
        cred.setUserTokenEncrypted(encryptedUserToken);
        cred.setHasUserToken(encryptedUserToken != null);
        cred.setTokenFingerprint(fingerprint);
        cred.setPollingIntervalSeconds(extracted.pollingIntervalSeconds());
        cred.setSingleFlight(extracted.singleFlight());
        cred.setTrackingPageUrl(extracted.trackingPageUrl());
        cred.setTokenStatus("UNKNOWN");
        cred.setExtractedAt(extracted.extractedAt() != null ? extracted.extractedAt() : now);
        cred.setValidationNotes(initialNote);
        cred.setCreatedBy(actor);
        cred.setUpdatedBy(actor);
        cred.setCreatedAt(now);
        cred.setUpdatedAt(now);
        cred.setVersion(0L);

        credentialMapper.insert(cred);
        return cred;
    }

    private PollExecutionResultDto validateCredentialInternal(
            FlightTrackingCredential cred,
            boolean bypassMinInterval,
            String experimentTag) {

        OffsetDateTime now = OffsetDateTime.now();
        if (!bypassMinInterval && minPollIntervalSeconds > 0 && cred.getLastUsedAt() != null) {
            long secondsSinceLast = Duration.between(cred.getLastUsedAt(), now).getSeconds();
            if (secondsSinceLast >= 0 && secondsSinceLast < minPollIntervalSeconds) {
                throw BusinessException.badRequest(
                        "POLL_RATE_LIMITED",
                        "Batas interval polling konservatif aktif: tunggu "
                                + (minPollIntervalSeconds - secondsSinceLast) + " detik sebelum melakukan polling ulang."
                );
            }
        }

        String decryptedToken = cryptoService.decrypt(cred.getTrackingTokenEncrypted());
        String decryptedUserToken = cred.getUserTokenEncrypted() != null
                ? cryptoService.decrypt(cred.getUserTokenEncrypted())
                : null;

        NormalizedPollPayload pollResult = providerAdapter.pollFlightTracking(decryptedToken, decryptedUserToken);

        // Distinguish between:
        // 1. HTTP success
        // 2. Valid tracking response
        // 3. Correct flight identity
        // 4. Valid operational data
        boolean httpSuccess = pollResult.httpStatus() >= 200 && pollResult.httpStatus() < 300;
        boolean validTrackingJson = pollResult.responseValid() && pollResult.normalizedFlight() != null;
        boolean identityMatches = validTrackingJson && verifyFlightIdentityMatch(
                cred.getFlightNumber(), pollResult.normalizedFlight().flightIdentifier()
        );
        boolean hasOperationalData = validTrackingJson && hasValidOperationalFields(pollResult.normalizedFlight());

        boolean fullValidationPassed = httpSuccess && validTrackingJson && identityMatches && hasOperationalData;
        String validationNote;
        String nextStatus;

        if (fullValidationPassed) {
            nextStatus = "ACTIVE";
            validationNote = "Validasi penuh berhasil: HTTP " + pollResult.httpStatus()
                    + ", JSON valid, identitas cocok (" + pollResult.normalizedFlight().flightIdentifier()
                    + "), dan atribut operasional tersedia.";
            supersedeOtherActiveCredentials(cred);
            cred.setLastValidatedAt(pollResult.responseTimestamp());
        } else {
            boolean wasPreviouslyValidated = cred.getLastValidatedAt() != null;
            if (!httpSuccess) {
                nextStatus = wasPreviouslyValidated ? "EXPIRED" : "INVALID";
                validationNote = "Gagal HTTP (" + pollResult.httpStatus() + "): "
                        + (pollResult.errorMessage() != null ? pollResult.errorMessage() : "Respons gagal.");
            } else if (!validTrackingJson) {
                nextStatus = wasPreviouslyValidated ? "EXPIRED" : "INVALID";
                validationNote = "HTTP 200 tetapi respons bukan data pelacakan yang valid (" + pollResult.parsingStatus() + "): "
                        + (pollResult.errorMessage() != null ? pollResult.errorMessage() : "");
            } else if (!identityMatches) {
                nextStatus = "INVALID";
                validationNote = "Ketidakcocokan identitas penerbangan: kredensial mengharapkan '"
                        + cred.getFlightNumber() + "' tetapi respons mengembalikan '"
                        + pollResult.normalizedFlight().flightIdentifier() + "'.";
            } else {
                nextStatus = "INVALID";
                validationNote = "Respons JSON tidak memuat atribut operasional keberangkatan/kedatangan/status yang bermakna.";
            }

            if ("EXPIRED".equals(nextStatus) && cred.getExpiresAt() == null) {
                cred.setExpiresAt(pollResult.responseTimestamp());
            }
        }

        cred.setLastUsedAt(pollResult.requestTimestamp());
        cred.setTokenStatus(nextStatus);
        cred.setValidationNotes(validationNote);
        cred.setUpdatedBy(currentActorId());
        cred.setUpdatedAt(OffsetDateTime.now());
        credentialMapper.updateById(cred);

        // Deduplicate immediate identical retry within 2 seconds if payload hash matches
        String payloadHash = computePayloadHash(pollResult.rawResponse());
        FlightTrackingPollLog savedLog = savePollLogAvoidingImmediateDuplicate(
                cred.getId(), pollResult, fullValidationPassed, payloadHash, experimentTag
        );

        return new PollExecutionResultDto(
                savedLog.getId(),
                cred.getId(),
                pollResult.flightIdentifier(),
                pollResult.requestTimestamp(),
                pollResult.responseTimestamp(),
                pollResult.responseDurationMs(),
                pollResult.httpStatus(),
                fullValidationPassed,
                pollResult.parsingStatus(),
                pollResult.rawResponse(),
                pollResult.parsedResponse(),
                pollResult.normalizedFlight(),
                pollResult.discoveredFields(),
                pollResult.errorMessage(),
                nextStatus,
                validationNote
        );
    }

    private void supersedeOtherActiveCredentials(FlightTrackingCredential newlyActive) {
        List<FlightTrackingCredential> others = credentialMapper.selectList(
                new LambdaQueryWrapper<FlightTrackingCredential>()
                        .eq(FlightTrackingCredential::getProvider, newlyActive.getProvider())
                        .eq(FlightTrackingCredential::getFlightNumber, newlyActive.getFlightNumber())
                        .eq(FlightTrackingCredential::getFlightDate, newlyActive.getFlightDate())
                        .eq(FlightTrackingCredential::getTokenStatus, "ACTIVE")
                        .ne(FlightTrackingCredential::getId, newlyActive.getId())
        );
        OffsetDateTime now = OffsetDateTime.now();
        for (FlightTrackingCredential other : others) {
            other.setTokenStatus("SUPERSEDED");
            other.setValidationNotes("Digantikan oleh kredensial aktif baru " + newlyActive.getId());
            other.setUpdatedAt(now);
            credentialMapper.updateById(other);
        }
    }

    private FlightTrackingPollLog savePollLogAvoidingImmediateDuplicate(
            String credentialId,
            NormalizedPollPayload pollResult,
            boolean fullValid,
            String payloadHash,
            String experimentTag) {

        List<FlightTrackingPollLog> latestList = pollLogMapper.selectList(
                new LambdaQueryWrapper<FlightTrackingPollLog>()
                        .eq(FlightTrackingPollLog::getCredentialId, credentialId)
                        .orderByDesc(FlightTrackingPollLog::getRequestTimestamp)
                        .last("LIMIT 1")
        );
        if (!latestList.isEmpty()) {
            FlightTrackingPollLog latest = latestList.getFirst();
            long millisBetween = Math.abs(Duration.between(latest.getRequestTimestamp(), pollResult.requestTimestamp()).toMillis());
            if (millisBetween < 1500
                    && Objects.equals(latest.getPayloadHash(), payloadHash)
                    && Objects.equals(latest.getHttpStatus(), pollResult.httpStatus())) {
                return latest;
            }
        }

        FlightTrackingPollLog logRow = new FlightTrackingPollLog();
        logRow.setId(UUID.randomUUID().toString());
        logRow.setCredentialId(credentialId);
        logRow.setRequestTimestamp(pollResult.requestTimestamp());
        logRow.setResponseTimestamp(pollResult.responseTimestamp());
        logRow.setHttpStatus(pollResult.httpStatus());
        logRow.setResponseDurationMs(pollResult.responseDurationMs());
        logRow.setResponseValid(fullValid);
        logRow.setParsingStatus(pollResult.parsingStatus());
        logRow.setFlightIdentifier(pollResult.flightIdentifier());
        logRow.setPayloadHash(payloadHash);
        logRow.setResponsePayload(pollResult.rawResponse());
        logRow.setNormalizedPayload(writeJsonSafely(pollResult.normalizedFlight()));
        logRow.setDiscoveredFieldsJson(writeJsonSafely(pollResult.discoveredFields()));
        logRow.setExperimentTag(experimentTag);
        logRow.setErrorMessage(pollResult.errorMessage());
        logRow.setCreatedAt(OffsetDateTime.now());
        pollLogMapper.insert(logRow);
        return logRow;
    }

    boolean verifyFlightIdentityMatch(String expectedFlightNumber, String actualFlightIdentifier) {
        if (expectedFlightNumber == null || expectedFlightNumber.isBlank()) {
            return actualFlightIdentifier != null && !actualFlightIdentifier.isBlank();
        }
        if (actualFlightIdentifier == null || actualFlightIdentifier.isBlank()) {
            return false;
        }
        String expClean = expectedFlightNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        String actClean = actualFlightIdentifier.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (expClean.equals(actClean) || actClean.contains(expClean) || expClean.contains(actClean)) {
            return true;
        }
        // Check IATA <-> ICAO prefix equivalence (e.g. GA190 <-> GIA190, ID6882 <-> BTK6882)
        if (expClean.length() >= 3) {
            String prefix2 = expClean.substring(0, 2);
            String numberPart = expClean.substring(2);
            String mappedIcao = INDONESIAN_AIRLINE_IATA_TO_ICAO.get(prefix2);
            if (mappedIcao != null && actClean.equals(mappedIcao + numberPart)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValidOperationalFields(NormalizedFlightSummary norm) {
        if (norm == null) return false;
        return (norm.flightStatus() != null && !norm.flightStatus().isBlank())
                || (norm.scheduledDeparture() != null && !norm.scheduledDeparture().isBlank())
                || (norm.scheduledArrival() != null && !norm.scheduledArrival().isBlank())
                || (norm.estimatedArrival() != null && !norm.estimatedArrival().isBlank());
    }

    private String resolveFlightNumber(String requestedFlightNumber, Booking booking, String url) {
        if (requestedFlightNumber != null && !requestedFlightNumber.isBlank()) {
            return normalizeFlightNumberCode(requestedFlightNumber);
        }
        if (booking != null && booking.getFlightNumber() != null && !booking.getFlightNumber().isBlank()) {
            return normalizeFlightNumberCode(booking.getFlightNumber());
        }
        if (url != null) {
            Matcher m = URL_FLIGHT_CODE_PATTERN.matcher(url);
            if (m.find()) {
                return normalizeFlightNumberCode(m.group(1));
            }
        }
        return "UNKNOWN-FLIGHT";
    }

    private LocalDate resolveFlightDate(LocalDate requestedDate, Booking booking) {
        if (requestedDate != null) {
            return requestedDate;
        }
        if (booking != null) {
            if (booking.getFlightScheduledAt() != null) {
                return booking.getFlightScheduledAt().toLocalDate();
            }
            if (booking.getRequestedPickupAt() != null) {
                return booking.getRequestedPickupAt().toLocalDate();
            }
        }
        return LocalDate.now();
    }

    private String normalizeFlightNumberCode(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private FlightTrackingCredential requireCredential(String id) {
        FlightTrackingCredential cred = credentialMapper.selectById(id);
        if (cred == null) {
            throw BusinessException.notFound("Kredensial pelacakan penerbangan tidak ditemukan: " + id);
        }
        return cred;
    }

    private Map<String, String> loadBookingCodeMap(List<FlightTrackingCredential> creds) {
        List<String> ids = creds.stream()
                .map(FlightTrackingCredential::getFlightId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>().in(Booking::getId, ids)).stream()
                .collect(Collectors.toMap(Booking::getId, Booking::getBookingCode, (a, b) -> a));
    }

    private String currentActorId() {
        var su = SecurityUtils.currentUserOrNull();
        return su != null ? su.userId() : "SYSTEM";
    }

    private CredentialSummaryDto toCredentialSummaryDto(FlightTrackingCredential c, String bookingCode) {
        return new CredentialSummaryDto(
                c.getId(),
                c.getFlightId(),
                bookingCode,
                c.getTripId(),
                c.getProvider(),
                c.getFlightNumber(),
                c.getFlightDate(),
                c.getTrackingTokenEncrypted() != null && !c.getTrackingTokenEncrypted().isBlank(),
                Boolean.TRUE.equals(c.getHasUserToken()),
                c.getTokenFingerprint(),
                c.getPollingIntervalSeconds(),
                c.getSingleFlight(),
                c.getTrackingPageUrl(),
                c.getTokenStatus(),
                c.getExtractedAt(),
                c.getLastValidatedAt(),
                c.getLastUsedAt(),
                c.getExpiresAt(),
                c.getValidationNotes(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private PollExecutionResultDto toPollExecutionResultDto(
            FlightTrackingPollLog l,
            String credStatus,
            String credValidationNotes) {
        Map<String, Object> parsed = null;
        NormalizedFlightSummary normalized = null;
        List<DiscoveredFieldItem> discovered = List.of();
        try {
            if (l.getResponsePayload() != null && l.getResponsePayload().trim().startsWith("{")) {
                parsed = objectMapper.readValue(l.getResponsePayload(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ignored) {
        }
        try {
            if (l.getNormalizedPayload() != null && !l.getNormalizedPayload().isBlank()) {
                normalized = objectMapper.readValue(l.getNormalizedPayload(), NormalizedFlightSummary.class);
            }
        } catch (Exception ignored) {
        }
        try {
            if (l.getDiscoveredFieldsJson() != null && !l.getDiscoveredFieldsJson().isBlank()) {
                discovered = objectMapper.readValue(l.getDiscoveredFieldsJson(), new TypeReference<List<DiscoveredFieldItem>>() {});
            }
        } catch (Exception ignored) {
        }

        return new PollExecutionResultDto(
                l.getId(),
                l.getCredentialId(),
                l.getFlightIdentifier(),
                l.getRequestTimestamp(),
                l.getResponseTimestamp(),
                l.getResponseDurationMs() != null ? l.getResponseDurationMs() : 0L,
                l.getHttpStatus() != null ? l.getHttpStatus() : 0,
                Boolean.TRUE.equals(l.getResponseValid()),
                l.getParsingStatus(),
                l.getResponsePayload(),
                parsed,
                normalized,
                discovered,
                l.getErrorMessage(),
                credStatus,
                credValidationNotes
        );
    }

    private String writeJsonSafely(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String computePayloadHash(String rawPayload) {
        if (rawPayload == null || rawPayload.isEmpty()) {
            return "empty";
        }
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(rawPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
