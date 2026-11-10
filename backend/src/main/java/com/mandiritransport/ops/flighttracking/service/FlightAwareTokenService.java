package com.mandiritransport.ops.flighttracking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.flighttracking.provider.FlightTrackingProvider.ExtractedTokenPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dedicated service for extracting `trackpollGlobals` metadata from a FlightAware flight page (Section 3).
 *
 * Technical safeguards:
 * - Isolates `<script>` blocks and specifically locates `trackpollGlobals` assignment (never blindly grabs
 *   the first occurrence of `"TOKEN"` on the page).
 * - Uses a stateful balanced-brace JavaScript object lexer that handles whitespace, comments, escaped quotes,
 *   and nested objects.
 * - Detects HTTP blocks, rate limits, or CAPTCHA / anti-bot challenges and halts immediately without attempting
 *   to bypass or evade provider restrictions.
 */
@Service
public class FlightAwareTokenService {

    private static final Logger log = LoggerFactory.getLogger(FlightAwareTokenService.class);

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
            "<script\\b[^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern TRACKPOLL_GLOBALS_ASSIGNMENT = Pattern.compile(
            "(?:var|let|const|window\\.)?\\s*trackpollGlobals\\s*=\\s*\\{",
            Pattern.DOTALL
    );

    private static final Set<String> KNOWN_GLOBAL_KEYS = Set.of(
            "TOKEN", "INTERVAL", "SINGLE_FLIGHT", "USERTOKEN"
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int httpTimeoutMs;
    private final Set<String> allowedHosts;

    public FlightAwareTokenService(
            ObjectMapper objectMapper,
            @Value("${app.flight-tracking.http-timeout-ms:10000}") int httpTimeoutMs,
            @Value("${app.flight-tracking.flightaware.allowed-hosts:flightaware.com,www.flightaware.com,id.flightaware.com,localhost,127.0.0.1}")
            String allowedHostsCsv) {
        this.objectMapper = objectMapper;
        this.httpTimeoutMs = Math.max(1000, httpTimeoutMs);
        this.allowedHosts = Arrays.stream(allowedHostsCsv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.httpTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public ExtractedTokenPayload extractFromUrl(String trackingPageUrl) {
        OffsetDateTime now = OffsetDateTime.now();
        if (trackingPageUrl == null || trackingPageUrl.isBlank()) {
            return new ExtractedTokenPayload(
                    false, "INVALID_URL", 0, trackingPageUrl,
                    null, null, null, null, List.of(), now,
                    "URL halaman pelacakan kosong."
            );
        }

        URI uri;
        try {
            uri = URI.create(trackingPageUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) || host == null) {
                return new ExtractedTokenPayload(
                        false, "INVALID_URL", 0, trackingPageUrl,
                        null, null, null, null, List.of(), now,
                        "URL harus menggunakan skema http/https yang valid."
                );
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);
            boolean hostPermitted = allowedHosts.stream()
                    .anyMatch(allowed -> lowerHost.equals(allowed) || lowerHost.endsWith("." + allowed));
            if (!hostPermitted) {
                return new ExtractedTokenPayload(
                        false, "INVALID_URL", 0, trackingPageUrl,
                        null, null, null, null, List.of(), now,
                        "Domain URL tidak termasuk dalam daftar host FlightAware yang diizinkan: " + host
                );
            }
        } catch (Exception e) {
            return new ExtractedTokenPayload(
                    false, "INVALID_URL", 0, trackingPageUrl,
                    null, null, null, null, List.of(), now,
                    "Format URL halaman pelacakan tidak valid: " + e.getMessage()
            );
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(httpTimeoutMs))
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String html = response.body() != null ? response.body() : "";

            if (isAccessRestrictedOrChallenge(status, html)) {
                log.warn("FlightAware page returned access restriction or anti-bot challenge (HTTP {}) for URL {}", status, uri);
                return new ExtractedTokenPayload(
                        false,
                        "ACCESS_RESTRICTED_OR_CHALLENGE",
                        status,
                        trackingPageUrl,
                        null, null, null, null, List.of(),
                        now,
                        "Halaman mengembalikan pembatasan akses, rate-limit, atau tantangan anti-bot (HTTP " + status
                                + "). Sesuai kebijakan kepatuhan, sistem tidak melakukan bypass."
                );
            }

            if (status < 200 || status >= 300) {
                return new ExtractedTokenPayload(
                        false,
                        "HTTP_ERROR",
                        status,
                        trackingPageUrl,
                        null, null, null, null, List.of(),
                        now,
                        "Permintaan halaman pelacakan gagal dengan status HTTP " + status + "."
                );
            }

            return parseTrackpollGlobalsFromHtml(html, trackingPageUrl, status, now);
        } catch (Exception e) {
            log.warn("HTTP error while fetching FlightAware page {}: {}", trackingPageUrl, e.getMessage());
            return new ExtractedTokenPayload(
                    false,
                    "HTTP_ERROR",
                    0,
                    trackingPageUrl,
                    null, null, null, null, List.of(),
                    now,
                    "Gagal mengambil halaman pelacakan (timeout atau kesalahan jaringan): " + e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Parses `trackpollGlobals` from raw HTML or script content.
     * Exposed package/publicly for deterministic unit testing of diverse HTML/JS structures.
     */
    public ExtractedTokenPayload parseTrackpollGlobalsFromHtml(
            String html,
            String sourceUrl,
            int httpStatus,
            OffsetDateTime extractedAt) {
        if (html == null || html.isBlank()) {
            return new ExtractedTokenPayload(
                    false, "MISSING_GLOBALS", httpStatus, sourceUrl,
                    null, null, null, null, List.of(), extractedAt,
                    "Respons halaman kosong dan tidak memuat variabel trackpollGlobals."
            );
        }

        List<String> candidateScripts = new ArrayList<>();
        Matcher scriptMatcher = SCRIPT_TAG_PATTERN.matcher(html);
        while (scriptMatcher.find()) {
            String scriptBody = scriptMatcher.group(1);
            if (scriptBody != null && scriptBody.contains("trackpollGlobals")) {
                candidateScripts.add(scriptBody);
            }
        }
        if (candidateScripts.isEmpty() && html.contains("trackpollGlobals")) {
            candidateScripts.add(html);
        }

        if (candidateScripts.isEmpty()) {
            return new ExtractedTokenPayload(
                    false, "MISSING_GLOBALS", httpStatus, sourceUrl,
                    null, null, null, null, List.of(), extractedAt,
                    "Variabel JavaScript trackpollGlobals tidak ditemukan pada halaman."
            );
        }

        List<String> extractedJsonObjects = new ArrayList<>();
        for (String script : candidateScripts) {
            Matcher assignMatcher = TRACKPOLL_GLOBALS_ASSIGNMENT.matcher(script);
            while (assignMatcher.find()) {
                int openBraceIndex = assignMatcher.end() - 1;
                String rawJsObject = extractBalancedJsObject(script, openBraceIndex);
                if (rawJsObject != null) {
                    extractedJsonObjects.add(rawJsObject);
                }
            }
        }

        if (extractedJsonObjects.isEmpty()) {
            return new ExtractedTokenPayload(
                    false, "MALFORMED_GLOBALS", httpStatus, sourceUrl,
                    null, null, null, null, List.of(), extractedAt,
                    "Deklarasi trackpollGlobals ditemukan tetapi blok objek JSON tidak seimbang atau rusak."
            );
        }

        String lastParseError = null;
        for (String rawObject : extractedJsonObjects) {
            try {
                String normalizedJson = normalizeJsObjectToJson(rawObject);
                Map<String, Object> map = objectMapper.readValue(
                        normalizedJson, new TypeReference<Map<String, Object>>() {}
                );
                String token = asNonBlankString(map.get("TOKEN"));
                if (token == null) {
                    lastParseError = "Objek trackpollGlobals ditemukan tetapi atribut TOKEN kosong atau tidak ada.";
                    continue;
                }

                Integer interval = asPositiveInteger(map.get("INTERVAL"));
                Boolean singleFlight = asNullableBoolean(map.get("SINGLE_FLIGHT"));
                String userToken = asNonBlankString(map.get("USERTOKEN"));

                List<String> additionalKeys = map.keySet().stream()
                        .filter(k -> !KNOWN_GLOBAL_KEYS.contains(k))
                        .sorted()
                        .toList();

                return new ExtractedTokenPayload(
                        true,
                        "EXTRACTED",
                        httpStatus,
                        sourceUrl,
                        token,
                        interval,
                        singleFlight,
                        userToken,
                        additionalKeys,
                        extractedAt,
                        "Berhasil mengekstrak metadata trackpollGlobals dari halaman FlightAware."
                );
            } catch (Exception e) {
                lastParseError = "Gagal mengurai JSON trackpollGlobals: " + e.getMessage();
            }
        }

        return new ExtractedTokenPayload(
                false,
                "MALFORMED_GLOBALS",
                httpStatus,
                sourceUrl,
                null, null, null, null, List.of(),
                extractedAt,
                lastParseError != null ? lastParseError : "Objek trackpollGlobals tidak memiliki struktur yang valid."
        );
    }

    /**
     * Stateful lexer that extracts a balanced `{ ... }` JavaScript object literal starting at `openBraceIdx`,
     * safely ignoring braces inside double/single-quoted strings and stripping JS comments so Jackson can
     * parse the resulting JSON cleanly.
     */
    String extractBalancedJsObject(String source, int openBraceIdx) {
        if (openBraceIdx < 0 || openBraceIdx >= source.length() || source.charAt(openBraceIdx) != '{') {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int depth = 0;
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;

        int len = source.length();
        for (int i = openBraceIdx; i < len; i++) {
            char c = source.charAt(i);
            char next = (i + 1 < len) ? source.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                    out.append(c);
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (escaped) {
                out.append(c);
                escaped = false;
                continue;
            }
            if (inDoubleQuote) {
                out.append(c);
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inSingleQuote) {
                out.append(c);
                if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (c == '/' && next == '/') {
                inLineComment = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                out.append(c);
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                out.append(c);
                continue;
            }

            out.append(c);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return out.toString();
                }
            }
        }
        return null;
    }

    /**
     * Removes trailing commas before closing `}` or `]` that occasionally appear in inline JS objects.
     */
    private String normalizeJsObjectToJson(String rawJsObject) {
        return rawJsObject.replaceAll(",\\s*([}\\]])", "$1");
    }

    private boolean isAccessRestrictedOrChallenge(int status, String body) {
        if (status == 401 || status == 403 || status == 429 || status == 503) {
            return true;
        }
        if (body == null || body.isEmpty()) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("cf-turnstile")
                || lower.contains("g-recaptcha")
                || lower.contains("h-captcha")
                || lower.contains("verify you are human")
                || lower.contains("attention required! | cloudflare");
    }

    private String asNonBlankString(Object val) {
        if (val == null) return null;
        String str = String.valueOf(val).trim();
        return str.isEmpty() || "null".equalsIgnoreCase(str) ? null : str;
    }

    private Integer asPositiveInteger(Object val) {
        if (val instanceof Number n) {
            int i = n.intValue();
            return i > 0 ? i : null;
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                int i = Integer.parseInt(s.trim());
                return i > 0 ? i : null;
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Boolean asNullableBoolean(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim()) || "1".equals(s.trim())) return true;
            if ("false".equalsIgnoreCase(s.trim()) || "0".equals(s.trim())) return false;
        }
        if (val instanceof Number n) {
            return n.intValue() != 0;
        }
        return null;
    }
}
