package com.mandiritransport.ops.flighttracking.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class FlightTrackingDtos {

    private FlightTrackingDtos() {
    }

    public record ExtractTokenRequest(
            @NotBlank(message = "URL halaman pelacakan FlightAware wajib diisi")
            String trackingPageUrl,
            String flightId,
            String tripId,
            String flightNumber,
            LocalDate flightDate,
            Boolean persistCredential,
            Boolean validateImmediately
    ) {
    }

    public record ReplaceCredentialRequest(
            @NotBlank(message = "URL halaman pelacakan pengganti wajib diisi")
            String trackingPageUrl
    ) {
    }

    public record ExecutePollRequest(
            String experimentTag,
            Boolean forceBypassMinInterval
    ) {
    }

    public record TokenExtractionSummaryDto(
            boolean extractionSuccess,
            String extractionStatus, // EXTRACTED, MISSING_GLOBALS, MALFORMED_GLOBALS, ACCESS_RESTRICTED_OR_CHALLENGE, HTTP_ERROR, INVALID_URL
            int httpStatus,
            String trackingPageUrl,
            boolean tokenPresent,
            String tokenFingerprint,
            Integer pollingIntervalSeconds,
            Boolean singleFlight,
            boolean userTokenPresent,
            List<String> additionalDiscoveredKeys,
            OffsetDateTime extractedAt,
            String diagnosticMessage,
            CredentialSummaryDto persistedCredential
    ) {
    }

    public record CredentialSummaryDto(
            String id,
            String flightId,
            String bookingCode,
            String tripId,
            String provider,
            String flightNumber,
            LocalDate flightDate,
            boolean tokenPresent,
            boolean userTokenPresent,
            String tokenFingerprint,
            Integer pollingIntervalSeconds,
            Boolean singleFlight,
            String trackingPageUrl,
            String tokenStatus,
            OffsetDateTime extractedAt,
            OffsetDateTime lastValidatedAt,
            OffsetDateTime lastUsedAt,
            OffsetDateTime expiresAt,
            String validationNotes,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record DiscoveredFieldItem(
            String jsonPath,
            String dataType,
            String sampleValue,
            String timestampFormat,
            String mappedRequirementCode
    ) {
    }

    public record NormalizedFlightSummary(
            String flightIdentifier,
            String flightStatus,
            String originCode,
            String destinationCode,
            String scheduledDeparture,
            String estimatedDeparture,
            String actualDeparture,
            Integer departureDelayMinutes,
            String scheduledArrival,
            String estimatedArrival,
            String actualArrival,
            Integer arrivalDelayMinutes,
            Double latitude,
            Double longitude,
            Integer altitudeFeet,
            Integer groundSpeedKts,
            String aircraftType,
            String aircraftRegistration,
            Boolean cancelled,
            Boolean diverted,
            String timestampTimezoneFormat,
            Map<String, String> sourceFieldMap
    ) {
    }

    public record PollExecutionResultDto(
            String pollLogId,
            String credentialId,
            String flightIdentifier,
            OffsetDateTime requestTimestamp,
            OffsetDateTime responseTimestamp,
            long responseDurationMs,
            int httpStatus,
            boolean responseValid,
            String parsingStatus,
            String rawResponse,
            Map<String, Object> parsedResponse,
            NormalizedFlightSummary normalizedFlight,
            List<DiscoveredFieldItem> discoveredFields,
            String errorMessage,
            String updatedCredentialStatus,
            String validationMessage
    ) {
    }

    public record FieldChangeItem(
            String jsonPath,
            String oldValue,
            String newValue,
            String operationalCategory,
            boolean operationallySignificant
    ) {
    }

    public record PollDiffComparisonDto(
            String olderPollLogId,
            OffsetDateTime olderTimestamp,
            String newerPollLogId,
            OffsetDateTime newerTimestamp,
            List<FieldChangeItem> addedFields,
            List<FieldChangeItem> removedFields,
            List<FieldChangeItem> changedValues,
            List<FieldChangeItem> operationallySignificantChanges,
            int unchangedFieldCount,
            List<String> sampleUnchangedPaths,
            String summaryText
    ) {
    }

    public record RequirementValidationItem(
            String requirementCode,
            String requirementName,
            String importance, // CRITICAL, HIGH, OPTIONAL
            String availabilityStatus, // VERIFIED_MEANINGFUL, FIELD_PRESENT_UNVERIFIED_VALUE, MISSING_NOT_OBSERVED
            List<String> actualFieldNames,
            String dataType,
            String exampleValue,
            boolean changesObservedBetweenPolls,
            String timestampFormat,
            String verificationNotes
    ) {
    }

    public record ExperimentFindingDto(
            String experimentCode,
            String title,
            String status, // VERIFIED_PASS, PARTIAL_OR_CONDITIONAL, FAILED_OR_RESTRICTED, PENDING_EVIDENCE
            int evidenceCount,
            String observedBehavior,
            String operationalConclusion
    ) {
    }

    public record FlightDataValidationReportDto(
            String provider,
            String scopeCredentialId,
            String scopeFlightNumber,
            int totalCredentialsEvaluated,
            int totalPollLogsEvaluated,
            int validPollLogsCount,
            int criticalRequirementsSupported,
            int criticalRequirementsTotal,
            boolean meetsOperationalRequirements,
            String overallReadinessVerdict,
            List<RequirementValidationItem> requirements,
            List<ExperimentFindingDto> experiments,
            List<String> commercialAndTechnicalRestrictions
    ) {
    }
}

