package com.mandiritransport.ops.booking.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Zero-cost manual flight information adapter (no external paid flight API dependency).
 */
@Component
public class ManualFlightInfoAdapter implements FlightInfoAdapter {

    @Override
    public FlightInfo resolveFlight(String flightNumber, OffsetDateTime scheduledAt) {
        if (flightNumber == null || flightNumber.isBlank()) {
            return new FlightInfo(null, scheduledAt, "MANUAL_NONE");
        }
        String clean = flightNumber.trim().toUpperCase().replaceAll("\\s+", "-");
        return new FlightInfo(clean, scheduledAt, "MANUAL_ENTRY");
    }
}
