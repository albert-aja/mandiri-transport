package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PricingAndBookingIntegrationTest extends BaseIntegrationTest {

    @Test
    void fiveTierPricingWaterfallAndDateExpirationResolveDeterministically() throws Exception {
        String ownerToken = loginAs("hendra");

        // Tier 1: CUSTOMER_ROUTE_AGREEMENT (Ibu Ratna Medan->KNO = Rp 220.000 within 2026-2027)
        mockMvc.perform(post("/api/v1/pricing/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "cust-ratna",
                                "routeId", "rte-mdn-kno",
                                "vehicleType", "MPV",
                                "tripType", "AIRPORT_DROPOFF",
                                "serviceDate", "2026-11-15"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingTierUsed").value("CUSTOMER_ROUTE_AGREEMENT"))
                .andExpect(jsonPath("$.data.suggestedFare").value(220000.0));

        // Expiration guard: Ibu Ratna in 2028 (after effective_to 2027-12-31) falls back to Tier 3 ROUTE_PRICING_RULE (Rp 250.000)
        mockMvc.perform(post("/api/v1/pricing/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "cust-ratna",
                                "routeId", "rte-mdn-kno",
                                "vehicleType", "MPV",
                                "tripType", "AIRPORT_DROPOFF",
                                "serviceDate", "2028-02-01"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingTierUsed").value("ROUTE_PRICING_RULE"))
                .andExpect(jsonPath("$.data.suggestedFare").value(250000.0));

        // Tier 2: CUSTOMER_GENERAL_AGREEMENT (PT Sawit Makmur 10% corporate discount on Medan->KNO 250k = Rp 225.000)
        mockMvc.perform(post("/api/v1/pricing/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "cust-sawit",
                                "routeId", "rte-mdn-kno",
                                "vehicleType", "MPV",
                                "tripType", "AIRPORT_DROPOFF",
                                "serviceDate", "2026-11-15"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingTierUsed").value("CUSTOMER_GENERAL_AGREEMENT"))
                .andExpect(jsonPath("$.data.suggestedFare").value(225000.0));

        // Tier 3: ROUTE_PRICING_RULE (Customer without agreement on Medan->KNO = Rp 250.000)
        mockMvc.perform(post("/api/v1/pricing/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "cust-marriott",
                                "routeId", "rte-mdn-kno",
                                "vehicleType", "MPV",
                                "tripType", "AIRPORT_DROPOFF",
                                "serviceDate", "2026-11-15"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingTierUsed").value("ROUTE_PRICING_RULE"))
                .andExpect(jsonPath("$.data.suggestedFare").value(250000.0));

        // Tier 4: STANDARD_CALCULATION (No route rule: MPV 25 km = 50.000 + 5.500 * 25 = Rp 187.500)
        mockMvc.perform(post("/api/v1/pricing/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "vehicleType", "MPV",
                                "tripType", "LOCAL",
                                "distanceKm", 25,
                                "serviceDate", "2026-11-15"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingTierUsed").value("STANDARD_CALCULATION"))
                .andExpect(jsonPath("$.data.suggestedFare").value(187500.0));

        // Tier 5: MANUAL_CUSTOM (Ad-hoc custom trip without route/distance/charter -> customFare Rp 375.000)
        mockMvc.perform(post("/api/v1/pricing/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripType", "CUSTOM",
                                "customFare", 375000
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingTierUsed").value("MANUAL_CUSTOM"))
                .andExpect(jsonPath("$.data.suggestedFare").value(375000.0));
    }

    @Test
    void quickBookingLocksImmutableAgreedFareSnapshotAndPairsRoundTripBaggageBuffer() throws Exception {
        String ownerToken = loginAs("hendra");

        // Create Round-Trip Medan -> KNO (08:00) and Return KNO -> Medan (19:00) with Multi-Stop
        Map<String, Object> req = new HashMap<>();
        req.put("customerId", "cust-ratna");
        req.put("bookingSource", "WHATSAPP");
        req.put("tripType", "ROUND_TRIP");
        req.put("routeId", "rte-mdn-kno");
        req.put("requestedPickupAt", "2026-11-15T08:00:00+07:00");
        req.put("returnPickupAt", "2026-11-15T19:00:00+07:00");
        req.put("pickupAddress", "Jl. Kartini No. 18, Medan Polonia");
        req.put("destinationAddress", "Bandara Kualanamu (KNO)");
        req.put("passengerCount", 2);
        req.put("luggageCount", 2);
        req.put("flightNumber", "GA 185");
        req.put("vehicleTypePreference", "MPV");
        req.put("agreedFare", 210000); // Overridden from 220,000 suggested -> triggers fare_override_audits!
        req.put("overrideReason", "Diskon tambahan langganan");
        req.put("stops", List.of(Map.of("stopAddress", "Toko Bika Ambon Zulaikha Mojopahit", "notes", "Beli oleh-oleh 10 menit")));

        MvcResult createdRes = mockMvc.perform(post("/api/v1/bookings/quick")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips.length()").value(2))
                .andReturn();

        JsonNode data = readJson(createdRes).path("data");
        JsonNode leg1 = data.path("trips").get(0);
        JsonNode leg2 = data.path("trips").get(1);

        String leg1TripId = leg1.path("id").asText();
        assertThat(leg1.path("agreedFareSnapshot").decimalValue()).isEqualByComparingTo("210000");
        assertThat(leg1.path("flightNumber").asText()).isEqualTo("GA-185");
        assertThat(leg1.path("stops").size()).isEqualTo(1);
        // Leg 1 MDN->KNO buffers: 08:00 - 15m = 07:45; 08:00 + 75m + 0m + 15m = 09:30
        assertThat(leg1.path("arrivalBufferMin").asInt()).isEqualTo(0);

        // Leg 2 KNO->MDN automatically paired with rte-kno-mdn -> 30-min baggage collection buffer!
        assertThat(leg2.path("routeId").asText()).isEqualTo("rte-kno-mdn");
        assertThat(leg2.path("arrivalBufferMin").asInt()).isEqualTo(30);
        assertThat(leg2.path("agreedFareSnapshot").decimalValue()).isEqualByComparingTo("230000");

        // Now mutate Ibu Ratna's customer pricing agreement to Rp 300.000 AND route rule to Rp 320.000
        mockMvc.perform(put("/api/v1/pricing/customer-agreements/cpa-01")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "cust-ratna",
                                "agreementName", "Harga Baru 2027",
                                "routeId", "rte-mdn-kno",
                                "vehicleType", "MPV",
                                "pricingMode", "FIXED_FARE",
                                "agreedFare", 300000,
                                "isActive", true
                        ))))
                .andExpect(status().isOk());

        // Verify Leg 1's agreedFareSnapshot remains 100% IMMUTABLE at Rp 210.000!
        mockMvc.perform(get("/api/v1/trips/" + leg1TripId)
                        .headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreedFareSnapshot").value(210000.0));
    }

    @Test
    void bookingTemplateSavesPrefillOnlyAndNeverSchedulesBookingsOrTrips() throws Exception {
        String ownerToken = loginAs("hendra");

        MvcResult beforeBookings = mockMvc.perform(get("/api/v1/bookings").headers(authHeader(ownerToken)))
                .andExpect(status().isOk()).andReturn();
        int countBefore = readJson(beforeBookings).path("data").size();

        mockMvc.perform(post("/api/v1/booking-templates")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Template Cepat Hotel Adi Mulia -> KNO",
                                "customerId", "cust-marriott",
                                "routeId", "rte-mdn-kno",
                                "tripType", "AIRPORT_DROPOFF",
                                "pickupAddress", "Hotel Adi Mulia Jl. Diponegoro",
                                "destinationAddress", "Bandara Kualanamu (KNO)",
                                "defaultPassengerCount", 2,
                                "defaultLuggageCount", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Template Cepat Hotel Adi Mulia -> KNO"));

        MvcResult afterBookings = mockMvc.perform(get("/api/v1/bookings").headers(authHeader(ownerToken)))
                .andExpect(status().isOk()).andReturn();
        assertThat(readJson(afterBookings).path("data").size()).isEqualTo(countBefore);
    }
}
