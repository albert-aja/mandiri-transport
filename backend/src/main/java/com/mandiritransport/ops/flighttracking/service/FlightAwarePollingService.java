package com.mandiritransport.ops.flighttracking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.DiscoveredFieldItem;
import com.mandiritransport.ops.flighttracking.dto.FlightTrackingDtos.NormalizedFlightSummary;
import com.mandiritransport.ops.flighttracking.provider.FlightTrackingProvider.NormalizedPollPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dedicated client for polling FlightAware's `trackpoll.rvt` endpoint (Section 4).
 *
 * Principles:
 * - Queries `https://www.flightaware.com/ajax/trackpoll.rvt?token={TOKEN}&locale=en_US&summary=1`
 *   without browser cookies to test standalone token validity.
 * - Never logs the plaintext token.
 * - Preserves raw response payloads for research inspection.
 * - Dynamically discovers and normalizes available flight fields without inventing missing data.
 */
@Service
public class FlightAwarePollingService {

    private static final Logger log = LoggerFactory.getLogger(FlightAwarePollingService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int httpTimeoutMs;
    private volatile String pollEndpointUrl;

    public FlightAwarePollingService(
            ObjectMapper objectMapper,
            @Value("${app.flight-tracking.http-timeout-ms:10000}") int httpTimeoutMs,
            @Value("${app.flight-tracking.flightaware.poll-url:https://www.flightaware.com/ajax/trackpoll.rvt}")
            String pollEndpointUrl) {
        this.objectMapper = objectMapper;
        this.httpTimeoutMs = Math.max(1000, httpTimeoutMs);
        this.pollEndpointUrl = pollEndpointUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.httpTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void setPollEndpointUrlForTesting(String pollEndpointUrl) {
        this.pollEndpointUrl = pollEndpointUrl;
    }

    public String getPollEndpointUrl() {
        return pollEndpointUrl;
    }

    public NormalizedPollPayload poll(String decryptedToken, String decryptedUserToken) {
        OffsetDateTime requestTs = OffsetDateTime.now();
        long startNanos = System.nanoTime();

        if (decryptedToken == null || decryptedToken.isBlank()) {
            return new NormalizedPollPayload(
                    null, requestTs, OffsetDateTime.now(), 0L, 0,
                    false, "HTTP_ERROR", null, null, null, List.of(),
                    "Token pelacakan kosong — permintaan dibatalkan."
            );
        }

        try {
            String encodedToken = URLEncoder.encode(decryptedToken.trim(), StandardCharsets.UTF_8);
            String separator = pollEndpointUrl.contains("?") ? "&" : "?";
            String fullUrl = pollEndpointUrl + separator + "token=" + encodedToken + "&locale=en_US&summary=1";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofMillis(httpTimeoutMs))
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .GET();

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            OffsetDateTime responseTs = OffsetDateTime.now();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            int status = response.statusCode();
            String rawBody = response.body() != null ? response.body() : "";

            return parseAndNormalizeResponse(rawBody, status, requestTs, responseTs, durationMs);
        } catch (Exception e) {
            OffsetDateTime responseTs = OffsetDateTime.now();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.warn("FlightAware trackpoll.rvt network error: {}", e.getClass().getSimpleName());
            return new NormalizedPollPayload(
                    null,
                    requestTs,
                    responseTs,
                    durationMs,
                    0,
                    false,
                    "NETWORK_ERROR",
                    null,
                    null,
                    null,
                    List.of(),
                    "Kesalahan jaringan atau timeout saat memanggil trackpoll.rvt: " + e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Parses and normalizes a raw response from `trackpoll.rvt`.
     * Never invents flight fields when data is missing.
     */
    @SuppressWarnings("unchecked")
    public NormalizedPollPayload parseAndNormalizeResponse(
            String rawBody,
            int httpStatus,
            OffsetDateTime requestTs,
            OffsetDateTime responseTs,
            long durationMs) {

        if (httpStatus == 401 || httpStatus == 403 || httpStatus == 429 || httpStatus == 503) {
            return new NormalizedPollPayload(
                    null, requestTs, responseTs, durationMs, httpStatus,
                    false, "ACCESS_RESTRICTED_OR_CHALLENGE", rawBody, null, null, List.of(),
                    "Endpoint mengembalikan pembatasan akses atau rate-limit (HTTP " + httpStatus + ")."
            );
        }

        if (httpStatus < 200 || httpStatus >= 300) {
            return new NormalizedPollPayload(
                    null, requestTs, responseTs, durationMs, httpStatus,
                    false, "HTTP_ERROR", rawBody, null, null, List.of(),
                    "Endpoint trackpoll.rvt mengembalikan status HTTP " + httpStatus + "."
            );
        }

        String trimmed = rawBody != null ? rawBody.trim() : "";
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
            boolean challenge = trimmed.toLowerCase(Locale.ROOT).contains("cloudflare")
                    || trimmed.toLowerCase(Locale.ROOT).contains("captcha");
            return new NormalizedPollPayload(
                    null, requestTs, responseTs, durationMs, httpStatus,
                    false,
                    challenge ? "ACCESS_RESTRICTED_OR_CHALLENGE" : "NON_JSON_RESPONSE",
                    rawBody, null, null, List.of(),
                    challenge
                            ? "Respons berupa halaman tantangan anti-bot / akses terbatas, bukan JSON."
                            : "Respons HTTP 200 bukan objek JSON yang valid (kemungkinan sesi berakhir atau dialihkan ke HTML)."
            );
        }

        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new NormalizedPollPayload(
                    null, requestTs, responseTs, durationMs, httpStatus,
                    false, "NON_JSON_RESPONSE", rawBody, null, null, List.of(),
                    "Gagal mengurai respons sebagai JSON: " + e.getMessage()
            );
        }

        List<DiscoveredFieldItem> discoveredFields = new ArrayList<>();
        flattenAndDiscoverFields("", parsed, discoveredFields);

        Map<String, Object> primaryFlightObj = locatePrimaryFlightObject(parsed);
        if (primaryFlightObj == null || primaryFlightObj.isEmpty()) {
            String errFromPayload = extractFirstString(parsed, "error", "message", "statusMessage");
            return new NormalizedPollPayload(
                    null, requestTs, responseTs, durationMs, httpStatus,
                    false, "VALID_JSON_UNEXPECTED_SCHEMA", rawBody, parsed, null, discoveredFields,
                    errFromPayload != null
                            ? "JSON valid tetapi memuat pesan kesalahan atau tanpa objek penerbangan: " + errFromPayload
                            : "JSON valid tetapi tidak ditemukan data penerbangan aktif pada objek 'flights'."
            );
        }

        NormalizedFlightSummary normalized = buildNormalizedFlightSummary(primaryFlightObj);
        boolean hasIdentOrStatus = (normalized.flightIdentifier() != null && !normalized.flightIdentifier().isBlank())
                || (normalized.flightStatus() != null && !normalized.flightStatus().isBlank());

        if (!hasIdentOrStatus) {
            return new NormalizedPollPayload(
                    null, requestTs, responseTs, durationMs, httpStatus,
                    false, "VALID_JSON_UNEXPECTED_SCHEMA", rawBody, parsed, normalized, discoveredFields,
                    "Objek penerbangan ditemukan tetapi tidak memiliki identitas penerbangan maupun status operasional."
            );
        }

        return new NormalizedPollPayload(
                normalized.flightIdentifier(),
                requestTs,
                responseTs,
                durationMs,
                httpStatus,
                true,
                "VALID_TRACKING_JSON",
                rawBody,
                parsed,
                normalized,
                discoveredFields,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> locatePrimaryFlightObject(Map<String, Object> root) {
        Object flightsObj = root.get("flights");
        if (flightsObj instanceof Map<?, ?> flightsMap && !flightsMap.isEmpty()) {
            for (Map.Entry<?, ?> entry : flightsMap.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> flightVal) {
                    Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) flightVal);
                    copy.putIfAbsent("_flightKey", String.valueOf(entry.getKey()));
                    return copy;
                }
            }
        }
        if (flightsObj instanceof List<?> flightsList && !flightsList.isEmpty()) {
            Object first = flightsList.getFirst();
            if (first instanceof Map<?, ?> flightVal) {
                return new LinkedHashMap<>((Map<String, Object>) flightVal);
            }
        }
        if (root.containsKey("ident") || root.containsKey("flightNumber") || root.containsKey("gateDepartureTimes")) {
            return root;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private NormalizedFlightSummary buildNormalizedFlightSummary(Map<String, Object> flight) {
        Map<String, String> sourceMap = new LinkedHashMap<>();

        String ident = pickFieldString(flight, sourceMap, "FLIGHT_IDENTIFICATION",
                "ident", "displayIdent", "iataIdent", "flightNumber", "code");
        if (ident == null && flight.get("_flightKey") instanceof String fk && fk.contains("-")) {
            ident = fk.substring(0, fk.indexOf('-'));
            sourceMap.put("FLIGHT_IDENTIFICATION", "flights.<key>");
        }

        String status = pickFieldString(flight, sourceMap, "FLIGHT_STATUS",
                "flightStatus", "status", "phase", "landingStatus");

        String originCode = extractAirportCode(flight.get("origin"));
        String destCode = extractAirportCode(flight.get("destination"));

        Map<String, Object> gateDep = asMap(flight.get("gateDepartureTimes"));
        Map<String, Object> takeoff = asMap(flight.get("takeoffTimes"));
        Map<String, Object> gateArr = asMap(flight.get("gateArrivalTimes"));
        Map<String, Object> landing = asMap(flight.get("landingTimes"));

        String schedDep = pickTimestamp(flight, gateDep, takeoff, sourceMap, "SCHEDULED_DEPARTURE",
                "scheduled", "published", "scheduledDeparture");
        String estDep = pickTimestamp(flight, gateDep, takeoff, sourceMap, "ESTIMATED_DEPARTURE",
                "estimated", "estimated", "estimatedDeparture");
        String actDep = pickTimestamp(flight, gateDep, takeoff, sourceMap, "ACTUAL_DEPARTURE",
                "actual", "actual", "actualDeparture");

        String schedArr = pickTimestamp(flight, gateArr, landing, sourceMap, "SCHEDULED_ARRIVAL",
                "scheduled", "published", "scheduledArrival");
        String estArr = pickTimestamp(flight, gateArr, landing, sourceMap, "ESTIMATED_ARRIVAL",
                "estimated", "estimated", "estimatedArrival");
        String actArr = pickTimestamp(flight, gateArr, landing, sourceMap, "ACTUAL_ARRIVAL",
                "actual", "actual", "actualArrival");

        Integer depDelayMin = computeOrPickDelayMinutes(flight, gateDep, "departureDelay", "delay", schedDep, estDep, actDep);
        Integer arrDelayMin = computeOrPickDelayMinutes(flight, gateArr, "arrivalDelay", "gateArrivalDelay", schedArr, estArr, actArr);
        if (arrDelayMin != null) {
            sourceMap.putIfAbsent("ARRIVAL_DELAY", gateArr != null && gateArr.containsKey("delay")
                    ? "gateArrivalTimes.delay" : "derived(estimated-scheduled)");
        }

        Map<String, Object> lastPos = asMap(flight.get("lastPosition"));
        if (lastPos == null) {
            lastPos = asMap(flight.get("position"));
        }
        Double lat = null;
        Double lon = null;
        Integer alt = null;
        Integer gs = null;
        if (lastPos != null) {
            lat = asDouble(lastPos.get("lat"), lastPos.get("latitude"));
            lon = asDouble(lastPos.get("lon"), lastPos.get("lng"), lastPos.get("longitude"));
            alt = asInt(lastPos.get("alt"), lastPos.get("altitude"));
            gs = asInt(lastPos.get("gs"), lastPos.get("groundspeed"), lastPos.get("speed"));
            if (lat != null && lon != null) {
                sourceMap.put("AIRCRAFT_LOCATION", "lastPosition.{lat,lon}");
            }
        }

        Map<String, Object> aircraft = asMap(flight.get("aircraft"));
        String acType = aircraft != null ? asString(aircraft.get("type"), aircraft.get("friendlyType")) : asString(flight.get("aircraftType"));
        String acReg = aircraft != null ? asString(aircraft.get("tail"), aircraft.get("registration")) : asString(flight.get("tail"), flight.get("registration"));
        if (acReg != null) {
            sourceMap.put("AIRCRAFT_REGISTRATION", aircraft != null ? "aircraft.tail" : "tail");
        }

        Boolean cancelled = asBoolean(flight.get("cancelled"), flight.get("isCancelled"));
        if (cancelled == null && status != null && status.toLowerCase(Locale.ROOT).contains("cancel")) {
            cancelled = true;
        }
        if (cancelled != null) {
            sourceMap.put("CANCELLATION_STATUS", "cancelled");
        }

        Boolean diverted = asBoolean(flight.get("diverted"), flight.get("isDiverted"));
        if (diverted == null && status != null && status.toLowerCase(Locale.ROOT).contains("divert")) {
            diverted = true;
        }
        if (diverted != null) {
            sourceMap.put("DIVERSION_INFORMATION", "diverted");
        }

        String tzFormat = detectTimestampFormatSample(gateDep, gateArr, flight);

        return new NormalizedFlightSummary(
                ident,
                status,
                originCode,
                destCode,
                schedDep,
                estDep,
                actDep,
                depDelayMin,
                schedArr,
                estArr,
                actArr,
                arrDelayMin,
                lat,
                lon,
                alt,
                gs,
                acType,
                acReg,
                cancelled,
                diverted,
                tzFormat,
                sourceMap
        );
    }

    private void flattenAndDiscoverFields(String prefix, Object node, List<DiscoveredFieldItem> out) {
        if (node == null || out.size() >= 180) {
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String nextPath = prefix.isEmpty() ? key : prefix + "." + key;
                flattenAndDiscoverFields(nextPath, entry.getValue(), out);
            }
        } else if (node instanceof List<?> list) {
            if (!list.isEmpty()) {
                flattenAndDiscoverFields(prefix + "[0]", list.getFirst(), out);
            }
        } else {
            String dataType = node instanceof Integer || node instanceof Long ? "INTEGER"
                    : node instanceof Number ? "NUMBER"
                    : node instanceof Boolean ? "BOOLEAN"
                    : "STRING";
            String sample = String.valueOf(node);
            if (sample.length() > 80) {
                sample = sample.substring(0, 77) + "...";
            }
            String tsFormat = classifyTimestampValue(prefix, node);
            String reqCode = mapPathToRequirementCode(prefix);
            out.add(new DiscoveredFieldItem(prefix, dataType, sample, tsFormat, reqCode));
        }
    }

    private String mapPathToRequirementCode(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ident") || lower.endsWith(".displayident") || lower.endsWith(".flightnumber") || lower.equals("ident")) {
            return "FLIGHT_IDENTIFICATION";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("scheduled")) {
            return "SCHEDULED_DEPARTURE";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("estimated")) {
            return "ESTIMATED_DEPARTURE";
        }
        if ((lower.contains("departure") || lower.contains("takeoff")) && lower.contains("actual")) {
            return "ACTUAL_DEPARTURE";
        }
        if (lower.endsWith("flightstatus") || lower.endsWith(".status") || lower.endsWith(".phase")) {
            return "FLIGHT_STATUS";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("scheduled")) {
            return "SCHEDULED_ARRIVAL";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("estimated")) {
            return "ESTIMATED_ARRIVAL";
        }
        if ((lower.contains("arrival") || lower.contains("landing")) && lower.contains("actual")) {
            return "ACTUAL_ARRIVAL";
        }
        if (lower.contains("arrival") && lower.contains("delay")) {
            return "ARRIVAL_DELAY";
        }
        if (lower.contains("position.lat") || lower.contains("position.lon") || lower.endsWith(".latitude") || lower.endsWith(".longitude")) {
            return "AIRCRAFT_LOCATION";
        }
        if (lower.endsWith("aircraft.tail") || lower.endsWith(".registration") || lower.endsWith(".tail")) {
            return "AIRCRAFT_REGISTRATION";
        }
        if (lower.endsWith("cancelled") || lower.endsWith("iscancelled")) {
            return "CANCELLATION_STATUS";
        }
        if (lower.endsWith("diverted") || lower.endsWith("isdiverted")) {
            return "DIVERSION_INFORMATION";
        }
        return null;
    }

    private String classifyTimestampValue(String path, Object val) {
        String lower = path.toLowerCase(Locale.ROOT);
        boolean looksTimeRelated = lower.contains("time") || lower.contains("scheduled")
                || lower.contains("estimated") || lower.contains("actual") || lower.contains("timestamp");
        if (!looksTimeRelated) {
            return "N/A";
        }
        if (val instanceof Number n) {
            long v = n.longValue();
            if (v >= 1_000_000_000L && v <= 4_000_000_000L) {
                return "UTC_EPOCH_SECONDS";
            }
            if (v > 1_000_000_000_000L) {
                return "UTC_EPOCH_MILLIS";
            }
        }
        if (val instanceof String s) {
            if (s.endsWith("Z") || s.matches(".*[+-]\\d{2}:\\d{2}$")) {
                return "ISO_8601_WITH_OFFSET";
            }
            return "STRING_LOCAL_OR_UNSPECIFIED";
        }
        return "UNKNOWN";
    }

    private String detectTimestampFormatSample(Map<String, Object> gateDep, Map<String, Object> gateArr, Map<String, Object> flight) {
        for (Map<String, Object> m : new Map[]{gateDep, gateArr, flight}) {
            if (m == null) continue;
            for (String k : List.of("scheduled", "estimated", "actual", "scheduledDeparture", "scheduledArrival")) {
                Object v = m.get(k);
                if (v != null) {
                    String fmt = classifyTimestampValue("time." + k, v);
                    if (!"N/A".equals(fmt) && !"UNKNOWN".equals(fmt)) {
                        return fmt;
                    }
                }
            }
        }
        return "UNVERIFIED";
    }

    private String pickTimestamp(
            Map<String, Object> flight,
            Map<String, Object> primaryBlock,
            Map<String, Object> secondaryBlock,
            Map<String, String> sourceMap,
            String reqCode,
            String blockKey,
            String altBlockKey,
            String flatKey) {
        if (primaryBlock != null) {
            String v = formatTimestampValue(primaryBlock.get(blockKey));
            if (v != null) {
                sourceMap.put(reqCode, (reqCode.contains("DEPARTURE") ? "gateDepartureTimes." : "gateArrivalTimes.") + blockKey);
                return v;
            }
            String vAlt = formatTimestampValue(primaryBlock.get(altBlockKey));
            if (vAlt != null) {
                sourceMap.put(reqCode, (reqCode.contains("DEPARTURE") ? "gateDepartureTimes." : "gateArrivalTimes.") + altBlockKey);
                return vAlt;
            }
        }
        if (secondaryBlock != null) {
            String v = formatTimestampValue(secondaryBlock.get(blockKey));
            if (v != null) {
                sourceMap.put(reqCode, (reqCode.contains("DEPARTURE") ? "takeoffTimes." : "landingTimes.") + blockKey);
                return v;
            }
        }
        String flat = formatTimestampValue(flight.get(flatKey));
        if (flat != null) {
            sourceMap.put(reqCode, flatKey);
        }
        return flat;
    }

    private String formatTimestampValue(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) {
            long val = n.longValue();
            if (val <= 0) return null;
            if (val >= 1_000_000_000L && val <= 4_000_000_000L) {
                return Instant.ofEpochSecond(val).atOffset(ZoneOffset.UTC).toString();
            }
            if (val > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(val).atOffset(ZoneOffset.UTC).toString();
            }
        }
        String s = String.valueOf(raw).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    private Integer computeOrPickDelayMinutes(
            Map<String, Object> flight,
            Map<String, Object> timeBlock,
            String flatDelayKey,
            String altFlatKey,
            String schedIso,
            String estIso,
            String actIso) {
        Object direct = timeBlock != null ? timeBlock.get("delay") : null;
        if (direct == null) direct = flight.get(flatDelayKey);
        if (direct == null) direct = flight.get(altFlatKey);
        if (direct instanceof Number n) {
            int rawVal = n.intValue();
            return Math.abs(rawVal) > 360 && rawVal % 60 == 0 ? rawVal / 60 : rawVal;
        }
        String targetIso = actIso != null ? actIso : estIso;
        if (schedIso != null && targetIso != null) {
            try {
                OffsetDateTime sched = OffsetDateTime.parse(schedIso);
                OffsetDateTime actualOrEst = OffsetDateTime.parse(targetIso);
                return (int) Duration.between(sched, actualOrEst).toMinutes();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String pickFieldString(Map<String, Object> map, Map<String, String> sourceMap, String reqCode, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    sourceMap.put(reqCode, k);
                    return s;
                }
            }
        }
        return null;
    }

    private String extractFirstString(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v).trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractAirportCode(Object airportNode) {
        if (airportNode instanceof Map<?, ?> m) {
            return asString(m.get("iata"), m.get("icao"), m.get("code"), m.get("friendlyLocation"));
        }
        return asString(airportNode);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        return obj instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private String asString(Object... candidates) {
        for (Object c : candidates) {
            if (c != null) {
                String s = String.valueOf(c).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    private Double asDouble(Object... candidates) {
        for (Object c : candidates) {
            if (c instanceof Number n) return n.doubleValue();
            if (c instanceof String s && !s.isBlank()) {
                try {
                    return Double.parseDouble(s.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Integer asInt(Object... candidates) {
        for (Object c : candidates) {
            if (c instanceof Number n) return n.intValue();
            if (c instanceof String s && !s.isBlank()) {
                try {
                    return Integer.parseInt(s.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Boolean asBoolean(Object... candidates) {
        for (Object c : candidates) {
            if (c instanceof Boolean b) return b;
            if (c instanceof Number n) return n.intValue() != 0;
            if (c instanceof String s) {
                if ("true".equalsIgnoreCase(s.trim()) || "1".equals(s.trim())) return true;
                if ("false".equalsIgnoreCase(s.trim()) || "0".equals(s.trim())) return false;
            }
        }
        return null;
    }
}
