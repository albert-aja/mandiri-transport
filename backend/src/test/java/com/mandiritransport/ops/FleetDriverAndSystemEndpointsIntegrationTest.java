package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FleetDriverAndSystemEndpointsIntegrationTest extends BaseIntegrationTest {

    @Test
    void healthAuthDriverFleetRouteAndNotificationEndpointsWorkEndToEnd() throws Exception {
        // 1. Public health check & dev account switcher
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));

        mockMvc.perform(get("/api/v1/auth/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));

        String ownerToken = loginAs("hendra");
        String budiToken = loginAs("budi");

        mockMvc.perform(post("/api/v1/auth/fcm-token")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fcmToken", "fcm-device-budi-medan-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(true));

        // 2. Driver creation, availability transition & unavailability windows
        MvcResult createDrv = mockMvc.perform(post("/api/v1/drivers")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "syahputra",
                                "fullName", "Syahputra Lubis",
                                "phone", "081265559900",
                                "licenseNumber", "SIM-A-MDN-8899",
                                "licenseExpiryDate", "2029-05-20",
                                "maxDailyHours", 10
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availabilityStatus").value("AVAILABLE"))
                .andReturn();
        String newDriverId = readJson(createDrv).path("data").path("id").asText();

        mockMvc.perform(put("/api/v1/drivers/" + newDriverId + "/availability")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "availabilityStatus", "TEMPORARILY_UNAVAILABLE",
                                "notes", "Istirahat makan siang 1 jam"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availabilityStatus").value("TEMPORARILY_UNAVAILABLE"));

        mockMvc.perform(post("/api/v1/drivers/" + newDriverId + "/unavailability-windows")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "statusType", "ON_LEAVE",
                                "reason", "Acara keluarga di Binjai",
                                "startAt", "2026-11-20T00:00:00+07:00",
                                "endAt", "2026-11-20T23:59:00+07:00"
                        ))))
                .andExpect(status().isOk());

        // 3. Vehicle creation, document expiry tracking, standalone expense & Route creation
        MvcResult createVeh = mockMvc.perform(post("/api/v1/vehicles")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "registrationNumber", "BK 9988 MT",
                                "nameModel", "Toyota Innova Zenix G",
                                "brand", "Toyota",
                                "manufactureYear", 2025,
                                "vehicleType", "MPV",
                                "passengerCapacity", 6,
                                "luggageCapacityBags", 4,
                                "currentOdometerKm", 12500.0,
                                "suitabilityTags", "AIRPORT,INTERCITY,CHARTER,VIP"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String newVehId = readJson(createVeh).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/vehicles/" + newVehId + "/documents")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "docType", "ANNUAL_TAX_STNK",
                                "docNumber", "STNK-BK9988MT",
                                "expiryDate", "2027-08-15",
                                "reminderDaysBefore", 30
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/vehicles/" + newVehId + "/expenses")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "INSURANCE",
                                "amount", 4200000,
                                "expenseDate", "2026-11-09",
                                "description", "Premi asuransi tahunan All-Risk"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/routes")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "MDN_BINJAI",
                                "name", "Medan Kota -> Binjai",
                                "originName", "Medan Kota",
                                "destinationName", "Binjai",
                                "typicalDistanceKm", 24.0,
                                "defaultDurationMin", 50
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("MDN_BINJAI"));

        // 4. Inline quick customer creation + HOURLY_CHARTER standard calculation + Cash Custody Summary + Notifications
        mockMvc.perform(post("/api/v1/bookings/quick")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newCustomerName", "Ibu Melati Siregar",
                                "newCustomerPhone", "081370008899",
                                "bookingSource", "PHONE_CALL",
                                "tripType", "HOURLY_CHARTER",
                                "charterHours", 4,
                                "vehicleTypePreference", "MPV",
                                "requestedPickupAt", "2026-11-18T10:00:00+07:00",
                                "pickupAddress", "Cambridge City Square, Medan",
                                "destinationAddress", "Keliling Dalam Kota Medan (4 Jam)"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.booking.pricingTierUsed").value("STANDARD_CALCULATION"))
                .andExpect(jsonPath("$.data.booking.suggestedFare").value(360000.0)); // 4h * Rp 90.000/h MPV charter rate

        mockMvc.perform(get("/api/v1/cash-custody/summary").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDriverHeldCash").value(870000.0)); // 220k Budi + 650k Andi

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(Map.of(
                                "reminderType", "UPCOMING_PICKUP",
                                "enabled", true,
                                "leadTimeMinutes", 45,
                                "sendFcmPush", false
                        )))))
                .andExpect(status().isOk());

        MvcResult budiNotifs = mockMvc.perform(get("/api/v1/notifications").headers(authHeader(budiToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = readJson(budiNotifs).path("data").path("notifications");
        if (!list.isEmpty()) {
            String notifId = list.get(0).path("id").asText();
            mockMvc.perform(post("/api/v1/notifications/" + notifId + "/acknowledge")
                            .headers(authHeader(budiToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
        }
        assertThat(readJson(budiNotifs).path("data").isObject()).isTrue();
    }
}
