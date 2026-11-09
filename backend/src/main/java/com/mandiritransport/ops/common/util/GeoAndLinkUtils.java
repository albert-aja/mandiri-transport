package com.mandiritransport.ops.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class GeoAndLinkUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoAndLinkUtils() {
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return EARTH_RADIUS_KM * c;
    }

    public static BigDecimal calculateDistanceKmDecimal(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double km = calculateDistanceKm(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
    }

    public static String buildGoogleMapsSearchUrl(BigDecimal lat, BigDecimal lng, String addressText) {
        if (lat != null && lng != null) {
            return "https://www.google.com/maps/search/?api=1&query="
                    + lat.stripTrailingZeros().toPlainString() + ","
                    + lng.stripTrailingZeros().toPlainString();
        }
        if (addressText != null && !addressText.isBlank()) {
            return "https://www.google.com/maps/search/?api=1&query=" + urlEncode(addressText.trim());
        }
        return null;
    }

    public static String buildGoogleMapsDirectionsUrl(BigDecimal destLat, BigDecimal destLng, String destinationAddress) {
        if (destLat != null && destLng != null) {
            return "https://www.google.com/maps/dir/?api=1&destination="
                    + destLat.stripTrailingZeros().toPlainString() + ","
                    + destLng.stripTrailingZeros().toPlainString();
        }
        if (destinationAddress != null && !destinationAddress.isBlank()) {
            return "https://www.google.com/maps/dir/?api=1&destination=" + urlEncode(destinationAddress.trim());
        }
        return null;
    }

    public static String normalizeIndonesianPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "";
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }
        if (digits.startsWith("0062")) {
            return digits.substring(2);
        }
        if (digits.startsWith("0")) {
            return "62" + digits.substring(1);
        }
        if (digits.startsWith("8")) {
            return "62" + digits;
        }
        return digits;
    }

    public static String buildWhatsappUrl(String phone, String text) {
        String normalized = normalizeIndonesianPhone(phone);
        if (normalized.isEmpty()) {
            return null;
        }
        String base = "https://wa.me/" + normalized;
        if (text == null || text.isBlank()) {
            return base;
        }
        return base + "?text=" + urlEncode(text);
    }

    public static String formatIdr(BigDecimal amount) {
        if (amount == null) {
            return "Rp 0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("id-ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        if (rounded.signum() < 0) {
            return "-Rp " + df.format(rounded.abs());
        }
        return "Rp " + df.format(rounded);
    }

    private static String urlEncode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
