package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripGpsRbacAndOfflineSyncIntegrationTest extends BaseIntegrationTest {

    @Test
    void enforcesActiveTripGpsBoundariesSupportsLateOfflineGpsSyncAndRejectsPostTripTracking() throws Exception {
        String budiToken = loginAs("budi");

        // 1. Cannot start tracking while trip is still ASSIGNED (not active yet)
        mockMvc.perform(post("/api/v1/trips/trp-201/tracking/start")
                        .headers(authHeader(budiToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TRACKING_NOT_ALLOWED_IN_STATUS"));

        // 2. Accept trip & start active tracking
        mockMvc.perform(post("/api/v1/trips/trp-201/status")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newStatus", "ON_THE_WAY"))))
                .andExpect(status().isOk());

        MvcResult startRes = mockMvc.perform(post("/api/v1/trips/trp-201/tracking/start")
                        .headers(authHeader(budiToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingActive").value(true))
                .andReturn();
        OffsetDateTime startedAt = OffsetDateTime.parse(readJson(startRes).path("data").path("trackingStartedAt").asText());

        // 3. Upload active GPS points + verify deduplication on clientPointId
        OffsetDateTime p1Time = startedAt.plusSeconds(5);
        OffsetDateTime p2Time = startedAt.plusSeconds(15);
        Map<String, Object> gpsBatch1 = Map.of("points", List.of(
                Map.of("clientPointId", "gps-201-1", "latitude", 3.6422, "longitude", 98.8853, "recordedAt", p1Time.toString()),
                Map.of("clientPointId", "gps-201-2", "latitude", 3.6100, "longitude", 98.7800, "recordedAt", p2Time.toString())
        ));

        mockMvc.perform(post("/api/v1/trips/trp-201/gps-points/batch")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gpsBatch1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptedCount").value(2))
                .andExpect(jsonPath("$.data.duplicateCount").value(0));

        // Replay duplicate batch -> acceptedCount=0, duplicateCount=2
        mockMvc.perform(post("/api/v1/trips/trp-201/gps-points/batch")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gpsBatch1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptedCount").value(0))
                .andExpect(jsonPath("$.data.duplicateCount").value(2));

        // 4. Complete trip -> auto-ends active tracking!
        MvcResult compRes = mockMvc.perform(post("/api/v1/trips/trp-201/status")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newStatus", "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingActive").value(false))
                .andReturn();
        OffsetDateTime endedAt = OffsetDateTime.parse(readJson(compRes).path("data").path("trackingEndedAt").asText());

        // 5. Reject GPS point recorded 10 minutes AFTER trackingEndedAt (no background surveillance outside active trip!)
        mockMvc.perform(post("/api/v1/trips/trp-201/gps-points/batch")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", List.of(
                                Map.of("clientPointId", "gps-after-end", "latitude", 3.5752, "longitude", 98.6745,
                                        "recordedAt", endedAt.plusMinutes(10).toString())
                        )))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GPS_OUTSIDE_TRACKING_WINDOW"));

        // 6. Accept late offline GPS point whose recordedAt WAS inside [trackingStartedAt, trackingEndedAt]!
        mockMvc.perform(post("/api/v1/trips/trp-201/gps-points/batch")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("points", List.of(
                                Map.of("clientPointId", "gps-offline-late-sync", "latitude", 3.5752, "longitude", 98.6745,
                                        "recordedAt", startedAt.plusSeconds(10).toString())
                        )))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptedCount").value(1));
    }

    @Test
    void enforcesStrictRoleBasedAccessControlSupabaseJwtAndCommunicationHelpers() throws Exception {
        String ownerToken = loginAs("hendra");
        String budiToken = loginAs("budi");

        // Owner adds a private internal note (visibleToDrivers = false) on cust-ratna
        mockMvc.perform(post("/api/v1/customers/cust-ratna/notes")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "OPERATIONAL",
                                "noteText", "Catatan internal tagihan bulanan khusus Owner",
                                "visibleToDrivers", false
                        ))))
                .andExpect(status().isOk());

        // Driver Budi can view cust-ratna (linked to trp-201) ONLY with visibleToDrivers == true notes!
        MvcResult custRes = mockMvc.perform(get("/api/v1/customers/cust-ratna")
                        .headers(authHeader(budiToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode notes = readJson(custRes).path("data").path("notes");
        assertThat(notes.size()).isEqualTo(2); // Only the 2 driver-visible notes, NOT the private note!
        for (JsonNode n : notes) {
            assertThat(n.path("visibleToDrivers").asBoolean()).isTrue();
        }

        // Driver Budi CANNOT view Ko Ahong (cust-ahong is assigned to Rudi, never Budi) -> 403 FORBIDDEN!
        mockMvc.perform(get("/api/v1/customers/cust-ahong").headers(authHeader(budiToken)))
                .andExpect(status().isForbidden());

        // Driver Budi CANNOT edit customer notes -> 403 FORBIDDEN!
        mockMvc.perform(post("/api/v1/customers/cust-ratna/notes")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("noteText", "Coba ubah"))))
                .andExpect(status().isForbidden());

        // Driver Budi CANNOT access Andi's trip trp-202 or Owner BI endpoints -> 403 FORBIDDEN!
        mockMvc.perform(get("/api/v1/trips/trp-202").headers(authHeader(budiToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/bi/explorer").headers(authHeader(budiToken)))
                .andExpect(status().isForbidden());

        // Supabase HS256 JWT authentication via sub = supabase_uid ("sb-owner-01") works seamlessly!
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", supabaseBearer("sb-owner-01", "OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("hendra"))
                .andExpect(jsonPath("$.data.role").value("OWNER"));

        // WhatsApp template click-to-chat rendering (never auto-sending) + Emergency alert + Inspection + Lost & Found
        mockMvc.perform(post("/api/v1/whatsapp-templates/render")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateCode", "INFO_SUPIR_BERANGKAT",
                                "tripId", "trp-201"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.whatsappUrl").value(org.hamcrest.Matchers.startsWith("https://wa.me/628116100101?text=")));

        mockMvc.perform(post("/api/v1/trips/trp-201/inspections")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("stage", "PRE_TRIP", "allPassed", true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trips/trp-201/lost-and-found")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemDescription", "Payung lipat hitam di kursi tengah"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/emergency-alerts")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripId", "trp-201",
                                "latitude", 3.6012,
                                "longitude", 98.7512,
                                "message", "Ban kempes di bahu jalan tol"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerWhatsappEmergencyUrl").value(org.hamcrest.Matchers.startsWith("https://wa.me/628116001001")));
    }

    @Test
    void offlineSyncStagesConcurrentReassignmentConflictWithoutLosingDriverCashPaymentAndDeduplicatesReplays() throws Exception {
        String ownerToken = loginAs("hendra");
        String budiToken = loginAs("budi");

        // Budi bootstraps trp-201 while online (captures initial version)
        MvcResult bootRes = mockMvc.perform(get("/api/v1/sync/driver-bootstrap")
                        .headers(authHeader(budiToken)))
                .andExpect(status().isOk())
                .andReturn();
        long initialVersion = 0L;
        for (JsonNode t : readJson(bootRes).path("data").path("trips")) {
            if ("trp-201".equals(t.path("id").asText())) {
                initialVersion = t.path("version").asLong();
            }
        }

        // While Budi is offline, Owner reassigns trp-201 to Driver Andi (drv-andi)!
        mockMvc.perform(post("/api/v1/trips/trp-201/assign")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "driverId", "drv-andi",
                                "vehicleId", "veh-avanza-01"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driverId").value("drv-andi"));

        // Budi regains connectivity and syncs offline mutations:
        // 1) TRIP_STATUS (COMPLETED, expectedVersion = initialVersion) -> MUST stage CONFLICT_STAGED without overwriting Andi!
        // 2) TRIP_PAYMENT (CASH Rp 230.000 collected by Budi) -> MUST be APPLIED and NEVER lost!
        Map<String, Object> syncReq = Map.of("mutations", List.of(
                Map.of(
                        "clientMutationId", "sync-mut-status-201",
                        "mutationType", "TRIP_STATUS",
                        "entityId", "trp-201",
                        "expectedVersion", initialVersion,
                        "payload", Map.of("newStatus", "COMPLETED")
                ),
                Map.of(
                        "clientMutationId", "sync-mut-pay-201",
                        "mutationType", "TRIP_PAYMENT",
                        "entityId", "trp-201",
                        "expectedVersion", initialVersion,
                        "payload", Map.of(
                                "amount", 230000,
                                "paymentMethod", "CASH",
                                "notes", "Tunai diterima Budi saat sinyal hilang"
                        )
                )
        ));

        mockMvc.perform(post("/api/v1/sync/mutations")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(syncReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].outcomeStatus").value("CONFLICT_STAGED"))
                .andExpect(jsonPath("$.data.results[1].outcomeStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data.openConflictsCount").value(1));

        // Server state of trp-201: still assigned to drv-andi (status ASSIGNED), AND payment + Budi cash custody IS recorded!
        mockMvc.perform(get("/api/v1/trips/trp-201").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driverId").value("drv-andi"))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.cashCustodyStatus").value("HELD_BY_DRIVER"))
                .andExpect(jsonPath("$.data.driverCashHeldAmount").value(230000.0));

        // Replay exact same offline sync batch -> BOTH return DUPLICATE_IGNORED (idempotent, zero double payment!)
        mockMvc.perform(post("/api/v1/sync/mutations")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(syncReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].outcomeStatus").value("DUPLICATE_IGNORED"))
                .andExpect(jsonPath("$.data.results[1].outcomeStatus").value("DUPLICATE_IGNORED"));

        mockMvc.perform(get("/api/v1/trips/trp-201").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPaidAmount").value(230000.0));

        // Owner reviews and resolves staged sync conflict
        MvcResult conflictsRes = mockMvc.perform(get("/api/v1/sync/conflicts").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        String conflictId = readJson(conflictsRes).path("data").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/sync/conflicts/" + conflictId + "/resolve")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resolutionStatus", "MERGED_MANUALLY",
                                "notes", "Dikonfirmasi: Budi menyelesaikan jemputan sebelum pengalihan terbaca"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolutionStatus").value("MERGED_MANUALLY"));
    }

    @Test
    void smartOpsAndBiExplorerEnforceSampleSizeGuardsDetectAnomaliesAndCrossFilterAccurately() throws Exception {
        String ownerToken = loginAs("hendra");

        // 13.2 Route BI: rte-mdn-kno has 5 completed trips (sufficientData = true), rte-mdn-ktj has 1 (< 5 -> sufficientData = false!)
        MvcResult routesRes = mockMvc.perform(get("/api/v1/bi/routes").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode routes = readJson(routesRes).path("data");
        boolean verifiedKnoSufficient = false;
        boolean verifiedKtjInsufficient = false;
        for (JsonNode r : routes) {
            if ("rte-mdn-kno".equals(r.path("routeId").asText())) {
                assertThat(r.path("sampleSize").asInt()).isEqualTo(5);
                assertThat(r.path("sufficientData").asBoolean()).isTrue();
                verifiedKnoSufficient = true;
            }
            if ("rte-mdn-ktj".equals(r.path("routeId").asText())) {
                assertThat(r.path("sampleSize").asInt()).isEqualTo(1);
                assertThat(r.path("sufficientData").asBoolean()).isFalse();
                verifiedKtjInsufficient = true;
            }
        }
        assertThat(verifiedKnoSufficient).isTrue();
        assertThat(verifiedKtjInsufficient).isTrue();

        // 13.6 Anomaly Detection: flags trp-104 (HIGH_FUEL_EXPENSE & LONG_TRIP_DURATION) and Fortuner maintenance
        MvcResult anomRes = mockMvc.perform(get("/api/v1/bi/anomalies").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode anomalies = readJson(anomRes).path("data").path("anomalies");
        assertThat(anomalies.size()).isGreaterThanOrEqualTo(4);
        assertThat(anomalies.toString()).contains("HIGH_FUEL_EXPENSE", "LONG_TRIP_DURATION", "UNUSUAL_MAINTENANCE_COST", "AGED_DRIVER_CASH");

        // 13.7 Vehicle Replacement Intelligence: Fortuner BK 4455 MT flagged EVALUATE_REPLACEMENT
        MvcResult replRes = mockMvc.perform(get("/api/v1/bi/vehicle-replacement").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        boolean fortunerFlagged = false;
        for (JsonNode v : readJson(replRes).path("data")) {
            if ("veh-fortuner-01".equals(v.path("vehicleId").asText())) {
                assertThat(v.path("replacementSignal").asText()).isEqualTo("EVALUATE_REPLACEMENT");
                fortunerFlagged = true;
            }
        }
        assertThat(fortunerFlagged).isTrue();

        // 13.1 Workload + 13.5 Customers + Section 14 Cross-filtered BI Explorer
        mockMvc.perform(get("/api/v1/bi/workload?date=2026-11-10").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDrivers").value(4));

        mockMvc.perform(get("/api/v1/bi/customers").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));

        mockMvc.perform(get("/api/v1/bi/explorer?routeId=rte-mdn-kno").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kpis.completedTrips").value(5))
                .andExpect(jsonPath("$.data.directQaInsights").isArray());
    }
}
