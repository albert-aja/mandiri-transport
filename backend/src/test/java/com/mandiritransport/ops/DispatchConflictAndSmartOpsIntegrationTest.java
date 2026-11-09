package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DispatchConflictAndSmartOpsIntegrationTest extends BaseIntegrationTest {

    @Test
    void detectsIndependentDriverVsVehicleConflictsLongDistanceRestBufferAndRequiresExplicitOverride() throws Exception {
        String ownerToken = loginAs("hendra");

        // Seeded trp-202 on 2026-11-10:
        // Route: rte-mdn-ktj (Medan -> Kuala Tanjung, required_rest_after_min = 60m)
        // Driver: drv-andi | Vehicle: veh-innova-01
        // Reserved window: 07:40 to 11:00 WIB -> Driver Andi needs mandatory rest until 12:00 WIB!

        // Scenario A: Simulate a new trip at 11:30 WIB (prep 15m -> reservedStart 11:15 WIB)
        // With Driver Andi + Vehicle Innova:
        // - Vehicle Innova (veh-innova-01) finished at 11:00 -> FREE (hasVehicleConflict == false)!
        // - Driver Andi (drv-andi) is in mandatory rest until 12:00 -> DRIVER CONFLICT (hasDriverConflict == true)!
        MvcResult simDriverRestConflict = mockMvc.perform(post("/api/v1/dispatch/simulate")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "routeId", "rte-mdn-kno",
                                "scheduledPickupAt", "2026-11-10T11:30:00+07:00",
                                "candidateDriverId", "drv-andi",
                                "candidateVehicleId", "veh-innova-01",
                                "passengerCount", 3,
                                "luggageCount", 2,
                                "tripType", "AIRPORT_DROPOFF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasDriverConflict").value(true))
                .andExpect(jsonPath("$.data.hasVehicleConflict").value(false))
                .andExpect(jsonPath("$.data.driverConflicts[0].type").value("DRIVER_REST_BUFFER_OVERLAP"))
                .andReturn();

        JsonNode simData = readJson(simDriverRestConflict).path("data");
        // Verify workload & suitability recommendations exist
        assertThat(simData.path("driverRecommendations").get(0).path("recommended").asBoolean()).isTrue();
        assertThat(simData.path("vehicleRecommendations").get(0).path("suitable").asBoolean()).isTrue();

        // Scenario B: Simulate on trp-203 (2026-11-10 09:00 WIB, 11 passengers to Berastagi)
        // Candidate Driver Budi (available & free at 09:00 on Nov 10 -> hasDriverConflict == false)
        // Candidate Vehicle Innova (assigned to trp-202 at 08:00 AND only 6 seats for 11 pax -> hasVehicleConflict == true!)
        mockMvc.perform(post("/api/v1/dispatch/simulate")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripId", "trp-203",
                                "candidateDriverId", "drv-budi",
                                "candidateVehicleId", "veh-innova-01"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasDriverConflict").value(false))
                .andExpect(jsonPath("$.data.hasVehicleConflict").value(true));

        // Scenario C: Attempt to assign trp-203 to ON_LEAVE driver drv-dedi + IN_MAINTENANCE veh-fortuner-01 without override -> HTTP 409 CONFLICT!
        mockMvc.perform(post("/api/v1/trips/trp-203/assign")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "driverId", "drv-dedi",
                                "vehicleId", "veh-fortuner-01",
                                "forceOverrideConfirmed", false
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SCHEDULE_CONFLICT"))
                .andExpect(jsonPath("$.data.hasDriverConflict").value(true))
                .andExpect(jsonPath("$.data.hasVehicleConflict").value(true));

        // Scenario D: Assign trp-203 to recommended conflict-free Hiace (veh-hiace-01, 14 seats) + Driver Rudi (drv-rudi) -> 200 OK!
        mockMvc.perform(post("/api/v1/trips/trp-203/assign")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "driverId", "drv-rudi",
                                "vehicleId", "veh-hiace-01",
                                "forceOverrideConfirmed", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.driverId").value("drv-rudi"))
                .andExpect(jsonPath("$.data.vehicleId").value("veh-hiace-01"));

        // Scenario E: Explicit override confirmation succeeds when dispatcher confirms override
        mockMvc.perform(post("/api/v1/trips/trp-203/assign")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "driverId", "drv-dedi",
                                "vehicleId", "veh-hiace-01",
                                "forceOverrideConfirmed", true,
                                "overrideReason", "Bang Dedi bersedia masuk sebentar atas izin keluarga"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driverId").value("drv-dedi"));
    }

    @Test
    void reschedulingTripPickupAutomaticallyUpdatesScheduledNotifications() throws Exception {
        String ownerToken = loginAs("hendra");
        String rudiToken = loginAs("rudi");

        // Assign trp-203 to Rudi -> schedules UPCOMING_PICKUP 60 mins before 09:00 (08:00)
        mockMvc.perform(post("/api/v1/trips/trp-203/assign")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "driverId", "drv-rudi",
                                "vehicleId", "veh-hiace-01"
                        ))))
                .andExpect(status().isOk());

        // Reschedule pickup to 11:00 WIB -> UPCOMING_PICKUP must reschedule to 10:00 WIB!
        mockMvc.perform(put("/api/v1/trips/trp-203/schedule")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduledPickupAt", "2026-11-10T11:00:00+07:00"
                        ))))
                .andExpect(status().isOk());

        MvcResult notifRes = mockMvc.perform(get("/api/v1/notifications")
                        .headers(authHeader(rudiToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode notifs = readJson(notifRes).path("data").path("notifications");
        boolean foundRescheduled = false;
        for (JsonNode n : notifs) {
            if ("UPCOMING_PICKUP".equals(n.path("reminderType").asText())
                    && "trp-203".equals(n.path("referenceId").asText())
                    && "SCHEDULED".equals(n.path("status").asText())) {
                assertThat(n.path("body").asText()).contains("11:00");
                foundRescheduled = true;
            }
        }
        assertThat(foundRescheduled).isTrue();
    }
}
