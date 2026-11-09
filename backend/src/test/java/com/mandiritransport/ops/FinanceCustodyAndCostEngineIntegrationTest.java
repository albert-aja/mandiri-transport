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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinanceCustodyAndCostEngineIntegrationTest extends BaseIntegrationTest {

    @Test
    void separatesPaymentStatusFromDriverCashCustodyAcrossPartialFullDisputedAndBankTransferFlows() throws Exception {
        String ownerToken = loginAs("hendra");
        String budiToken = loginAs("budi");

        // 1. Bank Transfer to Owner on trp-202 (remaining Rp 450.000 after Rp 200.000 DP = Rp 650.000 PAID, NO_DRIVER_CASH_DUE!)
        mockMvc.perform(post("/api/v1/trips/trp-202/payments")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 450000,
                                "paymentMethod", "BANK_TRANSFER",
                                "collectedByRole", "OWNER",
                                "referenceNumber", "TRF-SAWIT-LUNAS"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trips/trp-202").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.cashCustodyStatus").value("NO_DRIVER_CASH_DUE"))
                .andExpect(jsonPath("$.data.driverCashHeldAmount").value(0.0));

        // 2. Driver Budi collects Rp 230.000 CASH on trp-201 -> PAID + HELD_BY_DRIVER (Rp 230.000)
        MvcResult payRes = mockMvc.perform(post("/api/v1/trips/trp-201/payments")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 230000,
                                "paymentMethod", "CASH",
                                "clientMutationId", "mut-pay-trp201-cash"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String newPaymentId = readJson(payRes).path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/trips/trp-201").headers(authHeader(budiToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.cashCustodyStatus").value("HELD_BY_DRIVER"))
                .andExpect(jsonPath("$.data.driverCashHeldAmount").value(230000.0));

        // 3. Partial Cash Handover: Budi declares Rp 100.000 for newPaymentId, Owner confirms Rp 100.000 -> PARTIALLY_HANDED_OVER (Rp 130.000 remaining)
        MvcResult ho1Res = mockMvc.perform(post("/api/v1/cash-handovers")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "declaredAmount", 100000,
                                "paymentIds", List.of(newPaymentId),
                                "driverNotes", "Cicil setor 100rb dulu"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String ho1Id = readJson(ho1Res).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/cash-handovers/" + ho1Id + "/confirm")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "confirmedAmount", 100000,
                                "ownerNotes", "Diterima 100rb sebagian",
                                "markDisputed", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_HANDED_OVER"));

        mockMvc.perform(get("/api/v1/trips/trp-201").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashCustodyStatus").value("PARTIALLY_HANDED_OVER"))
                .andExpect(jsonPath("$.data.driverCashHeldAmount").value(130000.0));

        // 4. Complete remaining Rp 130.000 handover -> HANDED_OVER_CONFIRMED (Rp 0 remaining)
        MvcResult ho2Res = mockMvc.perform(post("/api/v1/cash-handovers")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "declaredAmount", 130000,
                                "paymentIds", List.of(newPaymentId),
                                "driverNotes", "Pelunasan sisa 130rb"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String ho2Id = readJson(ho2Res).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/cash-handovers/" + ho2Id + "/confirm")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "confirmedAmount", 130000,
                                "ownerNotes", "Sisa 130rb pas diterima",
                                "markDisputed", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HANDED_OVER_CONFIRMED"));

        mockMvc.perform(get("/api/v1/trips/trp-201").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashCustodyStatus").value("HANDED_OVER_CONFIRMED"))
                .andExpect(jsonPath("$.data.driverCashHeldAmount").value(0.0));

        // 5. Disputed Handover test on Andi's unsettled cash (pay-106 on trp-106 = Rp 650.000)
        String andiToken = loginAs("andi");
        MvcResult hoDisputeRes = mockMvc.perform(post("/api/v1/cash-handovers")
                        .headers(authHeader(andiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "declaredAmount", 650000,
                                "paymentIds", List.of("pay-106"),
                                "driverNotes", "Setor uang Kuala Tanjung"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String hoDisputeId = readJson(hoDisputeRes).path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/cash-handovers/" + hoDisputeId + "/confirm")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "confirmedAmount", 600000,
                                "ownerNotes", "Uang fisik baru 600rb, selisih 50rb perlu dicek",
                                "markDisputed", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPUTED"));

        mockMvc.perform(get("/api/v1/trips/trp-106").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cashCustodyStatus").value("DISPUTED"))
                .andExpect(jsonPath("$.data.driverCashHeldAmount").value(50000.0));

        // 6. Soft-void test: voiding DP payment pay-202 recalculates trp-202 without deleting payment record
        mockMvc.perform(post("/api/v1/payments/pay-202/void")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Koreksi nominal bukti transfer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isVoided").value(true));

        mockMvc.perform(get("/api/v1/trips/trp-202").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.data.totalPaidAmount").value(450000.0));
    }

    @Test
    void versionedCostEnginePreventsDoubleCountingAndPreservesHistoricalSnapshotsWhenFormulaV3Created() throws Exception {
        String ownerToken = loginAs("hendra");
        String budiToken = loginAs("budi");

        // Record actual FUEL (Rp 60.000) + TOLL (Rp 30.000) on trp-201 (agreedFare = 230.000), then complete with 40 km odometer delta
        mockMvc.perform(post("/api/v1/trips/trp-201/expenses")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "FUEL",
                                "amount", 60000,
                                "fuelLiters", 6.0
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trips/trp-201/expenses")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "TOLL",
                                "amount", 30000
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trips/trp-201/status")
                        .headers(authHeader(budiToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newStatus", "COMPLETED",
                                "odometerStartKm", 62100.0,
                                "odometerEndKm", 62140.0
                        ))))
                .andExpect(status().isOk());

        // Check profitability under Formula v2:
        // Distance = 40 km:
        // Estimated FUEL = 40 * 1350 = 54.000, BUT actual FUEL = 60.000 -> ACTUAL_OVERRIDES_ESTIMATE uses 60.000 ONLY (NOT 54k + 60k!)
        // Allocated components: Tire (80*40 = 3.200) + Oil (85*40 = 3.400) + Maint (250*40 = 10.000) + Depr (30.000) + Driver 20% of 230k (46.000) = 92.600
        // Actual expenses total = 60.000 + 30.000 = 90.000
        // Total effective cost = 90.000 + 92.600 = 182.600 (NO double counting of 54.000 estimated fuel!)
        // Actual profit = 230.000 - 182.600 = 47.400
        MvcResult profBefore = mockMvc.perform(get("/api/v1/trips/trp-201/profitability")
                        .headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotLocked").value(true))
                .andExpect(jsonPath("$.data.formulaVersionNumber").value(2))
                .andExpect(jsonPath("$.data.actualExpensesTotal").value(90000.0))
                .andExpect(jsonPath("$.data.allocatedOperatingCost").value(92600.0))
                .andExpect(jsonPath("$.data.totalEffectiveCost").value(182600.0))
                .andExpect(jsonPath("$.data.actualProfit").value(47400.0))
                .andReturn();
        assertThat(readJson(profBefore).path("data").path("items").size()).isGreaterThanOrEqualTo(8);

        // Now create Formula v3 effective 2026-12-01 with higher rates
        mockMvc.perform(post("/api/v1/cost-formulas")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Formula Biaya Operasional 2027 (v3)",
                                "effectiveFrom", "2026-12-01",
                                "isActive", true,
                                "components", List.of(
                                        Map.of(
                                                "componentCode", "FUEL",
                                                "name", "BBM Rp1.800/km",
                                                "calcType", "PER_KM",
                                                "reconciliationPolicy", "ACTUAL_OVERRIDES_ESTIMATE",
                                                "expenseCategoryLink", "FUEL",
                                                "rateValue", 1800
                                        ),
                                        Map.of(
                                                "componentCode", "DRIVER_COMPENSATION",
                                                "name", "Komisi Supir 25%",
                                                "calcType", "PERCENT_OF_FARE",
                                                "reconciliationPolicy", "ALLOCATED_ONLY",
                                                "rateValue", 0.25
                                        )
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNumber").value(3));

        // Verify BOTH historical seeded trp-101 AND newly completed trp-201 remain 100% IMMUTABLE on Formula v2!
        mockMvc.perform(get("/api/v1/trips/trp-201/profitability").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formulaVersionNumber").value(2))
                .andExpect(jsonPath("$.data.totalEffectiveCost").value(182600.0))
                .andExpect(jsonPath("$.data.actualProfit").value(47400.0));

        mockMvc.perform(get("/api/v1/trips/trp-101/profitability").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.formulaVersionNumber").value(2))
                .andExpect(jsonPath("$.data.totalEffectiveCost").value(171015.0))
                .andExpect(jsonPath("$.data.actualProfit").value(48985.0));
    }

    @Test
    void vehicleMaintenanceLogUpdatesOdometerMaintenanceScheduleAndRecordsAtomicVehicleExpense() throws Exception {
        String ownerToken = loginAs("hendra");

        mockMvc.perform(post("/api/v1/vehicles/veh-innova-01/maintenance-logs")
                        .headers(authHeader(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "maintenanceType", "OIL_CHANGE",
                                "serviceDate", "2026-11-09",
                                "odometerKm", 90100.0,
                                "vendorName", "Auto2000 Gatot Subroto",
                                "costAmount", 980000,
                                "downtimeHours", 2.5,
                                "description", "Ganti oli 90rb km",
                                "recordAsVehicleExpense", true
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vehicles/veh-innova-01").headers(authHeader(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentOdometerKm").value(90100.0))
                .andExpect(jsonPath("$.data.maintenanceSchedules[0].nextDueOdometerKm").value(100100.0));
    }
}
