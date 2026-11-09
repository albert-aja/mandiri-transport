package com.mandiritransport.ops;

import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.common.util.GeoAndLinkUtils;
import com.mandiritransport.ops.common.util.IdGenerator;
import com.mandiritransport.ops.finance.service.FormulaExpressionEvaluator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormulaAndUtilsUnitTest {

    @Test
    void safeFormulaEvaluatorComputesTireAndOilAllocationsAccurately() {
        Map<String, BigDecimal> tireVars = Map.of(
                "replacement_cost", new BigDecimal("4400000"),
                "expected_lifespan_km", new BigDecimal("55000"),
                "distance_km", new BigDecimal("41")
        );
        BigDecimal tireCost = FormulaExpressionEvaluator.evaluate(
                "(replacement_cost / expected_lifespan_km) * distance_km", tireVars);
        assertThat(tireCost).isEqualByComparingTo("3280");

        Map<String, BigDecimal> complexVars = Map.of(
                "distance_km", new BigDecimal("100"),
                "duration_hours", new BigDecimal("2.5"),
                "agreed_fare", new BigDecimal("650000")
        );
        BigDecimal complex = FormulaExpressionEvaluator.evaluate(
                "15000 + (distance_km * 350) + (duration_hours * 10000) - (-5000)", complexVars);
        assertThat(complex).isEqualByComparingTo("80000");
    }

    @Test
    void safeFormulaEvaluatorRejectsDivisionByZeroAndUnknownVariables() {
        assertThatThrownBy(() -> FormulaExpressionEvaluator.evaluate("1000 / 0", Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nol");

        assertThatThrownBy(() -> FormulaExpressionEvaluator.evaluate("distance_km * secret_var",
                Map.of("distance_km", BigDecimal.TEN)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("secret_var");
    }

    @Test
    void geoAndLinkUtilsBuildsZeroCostLinksAndNormalizesIndonesianPhones() {
        // Medan Polonia to KNO Airport (~23-26 km straight-line Haversine)
        double km = GeoAndLinkUtils.calculateDistanceKm(3.5752, 98.6745, 3.6422, 98.8853);
        assertThat(km).isBetween(22.0, 27.0);

        assertThat(GeoAndLinkUtils.normalizeIndonesianPhone("0812-6002-001")).isEqualTo("628126002001");
        assertThat(GeoAndLinkUtils.normalizeIndonesianPhone("+62 811 6001 001")).isEqualTo("628116001001");

        String waUrl = GeoAndLinkUtils.buildWhatsappUrl("08126002001", "Halo Pak Hendra");
        assertThat(waUrl).isEqualTo("https://wa.me/628126002001?text=Halo%20Pak%20Hendra");

        String mapsDir = GeoAndLinkUtils.buildGoogleMapsDirectionsUrl(
                new BigDecimal("3.6422000"), new BigDecimal("98.8853000"), "Bandara Kualanamu");
        assertThat(mapsDir).isEqualTo("https://www.google.com/maps/dir/?api=1&destination=3.6422,98.8853");

        assertThat(GeoAndLinkUtils.formatIdr(new BigDecimal("220000"))).isEqualTo("Rp 220.000");
        assertThat(IdGenerator.bookingCode(OffsetDateTime.parse("2026-11-10T08:00:00+07:00")))
                .startsWith("BKG-20261110-");
    }
}
