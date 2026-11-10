package com.mandiritransport.ops.flighttracking.provider;

import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.DiscoveredFieldItem;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.NormalizedFlightSummary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Provider-agnostic interface for external flight tracking adapters (Section 11).
 * Allows replacing or supplementing FlightAware with licensed aviation data providers
 * (e.g. FlightAware AeroAPI, OAG, Cirium, AviationStack) without altering downstream
 * Mandiri Transport flight management or future notification engines.
 */
public interface FlightTrackingProvider {

    String providerCode();

    record ExtractedTokenPayload(
            boolean success,
            String status,
            int httpStatus,
            String trackingPageUrl,
            String rawToken,
            Integer pollingIntervalSeconds,
            Boolean singleFlight,
            String rawUserToken,
            List<String> additionalDiscoveredKeys,
            OffsetDateTime extractedAt,
            String diagnosticMessage
    ) {
        @Override
        public String toString() {
            return "ExtractedTokenPayload{" +
                    "success=" + success +
                    ", status='" + status + '\'' +
                    ", httpStatus=" + httpStatus +
                    ", tokenPresent=" + (rawToken != null && !rawToken.isBlank()) +
                    ", pollingIntervalSeconds=" + pollingIntervalSeconds +
                    ", singleFlight=" + singleFlight +
                    ", userTokenPresent=" + (rawUserToken != null && !rawUserToken.isBlank()) +
                    '}';
        }
    }

    record NormalizedPollPayload(
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
            String errorMessage
    ) {
    }

    ExtractedTokenPayload extractTrackingMetadata(String trackingPageUrl);

    NormalizedPollPayload pollFlightTracking(String decryptedToken, String decryptedUserToken);
}

