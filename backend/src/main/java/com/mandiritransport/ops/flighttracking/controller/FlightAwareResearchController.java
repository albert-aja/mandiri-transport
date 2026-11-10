package com.mandiritransport.ops.flighttracking.controller;

import com.mandiritransport.ops.common.api.ApiResponse;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.CredentialSummaryDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.ExecutePollRequest;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.ExtractTokenRequest;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.FlightDataValidationReportDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.PollDiffComparisonDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.PollExecutionResultDto;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.ReplaceCredentialRequest;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.TokenExtractionSummaryDto;
import com.mandiritransport.ops.flighttracking.service.FlightAwareCredentialService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for the Admin FlightAware Research Console (Section 7).
 * Restricted strictly to `OWNER` role (`@PreAuthorize("hasRole('OWNER')")`).
 * Never exposes decrypted tracking tokens in any endpoint response.
 */
@RestController
@RequestMapping("/api/v1/integrations/flightaware")
@PreAuthorize("hasRole('OWNER')")
public class FlightAwareResearchController {

    private final FlightAwareCredentialService credentialService;

    public FlightAwareResearchController(FlightAwareCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping("/extract")
    public ApiResponse<TokenExtractionSummaryDto> extractToken(@Valid @RequestBody ExtractTokenRequest req) {
        return ApiResponse.ok(credentialService.extractToken(req));
    }

    @GetMapping("/credentials")
    public ApiResponse<List<CredentialSummaryDto>> listCredentials(
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) String tokenStatus) {
        return ApiResponse.ok(credentialService.listCredentials(flightNumber, tokenStatus));
    }

    @GetMapping("/credentials/history")
    public ApiResponse<List<CredentialSummaryDto>> getCredentialHistory(
            @RequestParam String flightNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate) {
        return ApiResponse.ok(credentialService.getCredentialHistoryForFlight(flightNumber, flightDate));
    }

    @PostMapping("/credentials/{id}/poll")
    public ApiResponse<PollExecutionResultDto> executePoll(
            @PathVariable String id,
            @RequestBody(required = false) ExecutePollRequest req) {
        return ApiResponse.ok(credentialService.executePollAndValidate(id, req));
    }

    @PostMapping("/credentials/{id}/revalidate")
    public ApiResponse<CredentialSummaryDto> revalidateCredential(@PathVariable String id) {
        return ApiResponse.ok(credentialService.revalidateCredential(id));
    }

    @PostMapping("/credentials/{id}/replace")
    public ApiResponse<CredentialSummaryDto> replaceCredential(
            @PathVariable String id,
            @Valid @RequestBody ReplaceCredentialRequest req) {
        return ApiResponse.ok(credentialService.replaceCredential(id, req.trackingPageUrl()));
    }

    @PostMapping("/credentials/{id}/disable")
    public ApiResponse<CredentialSummaryDto> disableCredential(@PathVariable String id) {
        return ApiResponse.ok(credentialService.disableCredential(id));
    }

    @GetMapping("/poll-logs")
    public ApiResponse<List<PollExecutionResultDto>> listPollLogs(
            @RequestParam(required = false) String credentialId) {
        return ApiResponse.ok(credentialService.listPollLogs(credentialId));
    }

    @GetMapping("/poll-logs/compare")
    public ApiResponse<PollDiffComparisonDto> comparePollLogs(
            @RequestParam String olderPollLogId,
            @RequestParam String newerPollLogId) {
        return ApiResponse.ok(credentialService.comparePollLogs(olderPollLogId, newerPollLogId));
    }

    @GetMapping("/credentials/{id}/diff-latest")
    public ApiResponse<PollDiffComparisonDto> compareLatestTwoPolls(@PathVariable String id) {
        return ApiResponse.ok(credentialService.compareLatestTwoPollsForCredential(id));
    }

    @GetMapping("/validation-report")
    public ApiResponse<FlightDataValidationReportDto> getValidationReport(
            @RequestParam(required = false) String credentialId) {
        return ApiResponse.ok(credentialService.generateValidationReport(credentialId));
    }

    @PostMapping("/poll-logs/purge-expired")
    public ApiResponse<Map<String, Object>> purgeExpiredLogs() {
        int deleted = credentialService.purgeExpiredPollLogs();
        return ApiResponse.ok(Map.of("purgedCount", deleted));
    }
}

