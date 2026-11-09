package com.mandiritransport.ops.booking.service;

import java.time.OffsetDateTime;

public interface FlightInfoAdapter {

    record FlightInfo(String normalizedFlightNumber, OffsetDateTime scheduledAt, String sourceNote) {
    }

    FlightInfo resolveFlight(String flightNumber, OffsetDateTime scheduledAt);
}
