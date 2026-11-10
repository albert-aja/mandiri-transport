package com.mandiritransport.ops.flighttracking.service;

import com.mandiritransport.ops.flighttracking.provider.FlightTrackingProvider;
import org.springframework.stereotype.Component;

/**
 * FlightAware implementation of {@link FlightTrackingProvider} (Section 11).
 * Keeps provider-specific extraction and polling encapsulated behind a replaceable boundary.
 */
@Component
public class FlightAwareProviderAdapter implements FlightTrackingProvider {

    public static final String PROVIDER_FLIGHTAWARE = "FLIGHTAWARE";

    private final FlightAwareTokenService tokenService;
    private final FlightAwarePollingService pollingService;

    public FlightAwareProviderAdapter(
            FlightAwareTokenService tokenService,
            FlightAwarePollingService pollingService) {
        this.tokenService = tokenService;
        this.pollingService = pollingService;
    }

    @Override
    public String providerCode() {
        return PROVIDER_FLIGHTAWARE;
    }

    @Override
    public ExtractedTokenPayload extractTrackingMetadata(String trackingPageUrl) {
        return tokenService.extractFromUrl(trackingPageUrl);
    }

    @Override
    public NormalizedPollPayload pollFlightTracking(String decryptedToken, String decryptedUserToken) {
        return pollingService.poll(decryptedToken, decryptedUserToken);
    }
}

