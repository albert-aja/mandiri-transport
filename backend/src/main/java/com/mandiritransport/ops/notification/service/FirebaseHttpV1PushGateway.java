package com.mandiritransport.ops.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FirebaseHttpV1PushGateway implements PushNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseHttpV1PushGateway.class);
    private static final String FCM_OAUTH_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

    private record ServiceAccountCredentials(
            String projectId,
            String clientEmail,
            PrivateKey privateKey,
            String tokenUri,
            String source
    ) {
    }

    private record CachedAccessToken(String token, long refreshAfterEpochSec) {
        boolean isValid(long nowEpochSec) {
            return token != null && !token.isBlank() && nowEpochSec < refreshAfterEpochSec;
        }
    }

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile boolean fcmEnabled;
    private volatile String configuredProjectId;
    private volatile String apiBaseUrl;
    private volatile String androidChannelId;
    private volatile ServiceAccountCredentials credentials;
    private volatile CachedAccessToken cachedToken;

    public FirebaseHttpV1PushGateway(
            ObjectMapper objectMapper,
            @Value("${app.fcm.enabled:true}") boolean fcmEnabled,
            @Value("${app.fcm.project-id:mandiri-transport-ops}") String configuredProjectId,
            @Value("${app.fcm.credentials-path:./firebase-service-account.json}") String credentialsPath,
            @Value("${app.fcm.credentials-json:}") String credentialsJson,
            @Value("${app.fcm.api-base-url:https://fcm.googleapis.com}") String apiBaseUrl,
            @Value("${app.fcm.android-channel-id:mandiri_ops_high_importance}") String androidChannelId
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.fcmEnabled = fcmEnabled;
        this.configuredProjectId = configuredProjectId;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.androidChannelId = androidChannelId;
        this.credentials = loadCredentialsSafely(credentialsJson, credentialsPath);
    }

    /**
     * Allows runtime or integration-test reconfiguration of Service Account JSON and FCM endpoint.
     */
    public synchronized void reconfigureForRuntime(boolean enabled,
                                                   String serviceAccountJson,
                                                   String overrideApiBaseUrl) {
        this.fcmEnabled = enabled;
        if (overrideApiBaseUrl != null && !overrideApiBaseUrl.isBlank()) {
            this.apiBaseUrl = stripTrailingSlash(overrideApiBaseUrl);
        }
        this.cachedToken = null;
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            this.credentials = null;
        } else {
            this.credentials = parseServiceAccountJson(serviceAccountJson, "RUNTIME_CONFIG");
        }
    }

    @Override
    public GatewayDiagnostics getDiagnostics() {
        ServiceAccountCredentials creds = this.credentials;
        boolean ready = fcmEnabled && creds != null;
        String effectiveProject = creds != null && creds.projectId() != null && !creds.projectId().isBlank()
                ? creds.projectId()
                : configuredProjectId;
        String maskedEmail = creds != null ? maskEmail(creds.clientEmail()) : null;
        String source = creds != null ? creds.source() : "NONE";
        String summary;
        if (!fcmEnabled) {
            summary = "FCM dinonaktifkan melalui konfigurasi (FCM_ENABLED=false) — menggunakan kotak masuk aplikasi.";
        } else if (creds == null) {
            summary = "Mode Siaga: Kredensial Firebase Service Account belum ditemukan — notifikasi disimpan di kotak masuk aplikasi & siap mengirim push real-time begitu firebase-service-account.json dipasang.";
        } else {
            summary = "FCM HTTP v1 Live Aktif (Proyek: " + effectiveProject + ") — siap mengirim push latar belakang real-time.";
        }
        return new GatewayDiagnostics(
                fcmEnabled,
                ready,
                source,
                effectiveProject,
                maskedEmail,
                androidChannelId,
                summary
        );
    }

    @Override
    public PushDeliveryResult sendPush(String fcmToken,
                                       String notificationId,
                                       String reminderType,
                                       String title,
                                       String body,
                                       String referenceType,
                                       String referenceId) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return PushDeliveryResult.inAppOnly("Token perangkat kosong.");
        }
        if (!fcmEnabled) {
            return PushDeliveryResult.inAppOnly("FCM_ENABLED=false");
        }
        ServiceAccountCredentials creds = this.credentials;
        if (creds == null) {
            log.debug("FCM credentials not configured yet; skipping remote HTTP v1 call for notification={}", notificationId);
            return PushDeliveryResult.inAppOnly("Service account belum dikonfigurasi.");
        }

        try {
            String accessToken = obtainAccessToken(creds);
            String effectiveProjectId = creds.projectId() != null && !creds.projectId().isBlank()
                    ? creds.projectId()
                    : configuredProjectId;

            Map<String, String> dataPayload = new LinkedHashMap<>();
            dataPayload.put("notificationId", safeStr(notificationId));
            dataPayload.put("reminderType", safeStr(reminderType));
            dataPayload.put("referenceType", safeStr(referenceType));
            dataPayload.put("referenceId", safeStr(referenceId));
            dataPayload.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("token", fcmToken.trim());
            message.put("notification", Map.of(
                    "title", safeStr(title),
                    "body", safeStr(body)
            ));
            message.put("data", dataPayload);
            message.put("android", Map.of(
                    "priority", "HIGH",
                    "notification", Map.of(
                            "channel_id", androidChannelId,
                            "sound", "default",
                            "click_action", "FLUTTER_NOTIFICATION_CLICK"
                    )
            ));
            message.put("apns", Map.of(
                    "headers", Map.of("apns-priority", "10"),
                    "payload", Map.of("aps", Map.of("sound", "default"))
            ));

            String requestJson = objectMapper.writeValueAsString(Map.of("message", message));
            URI sendUri = URI.create(apiBaseUrl + "/v1/projects/" + effectiveProjectId + "/messages:send");

            HttpRequest request = HttpRequest.newBuilder(sendUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String respBody = response.body() != null ? response.body() : "";

            if (status >= 200 && status < 300) {
                JsonNode root = respBody.isBlank() ? null : objectMapper.readTree(respBody);
                String msgName = root != null && root.hasNonNull("name") ? root.get("name").asText() : "fcm-ok";
                log.info("Live FCM HTTP v1 push delivered: messageId={} reminderType={}", msgName, reminderType);
                return PushDeliveryResult.sent(msgName);
            }

            boolean unregistered = status == 404
                    || respBody.contains("UNREGISTERED")
                    || respBody.contains("registration-token-not-registered");
            String errSummary = "HTTP " + status + (respBody.isBlank() ? "" : ": " + truncate(respBody, 240));
            log.warn("FCM HTTP v1 push failed (unregistered={}): {}", unregistered, errSummary);
            return PushDeliveryResult.failed(unregistered, errSummary);
        } catch (Exception ex) {
            log.warn("FCM HTTP v1 dispatch exception for notification={}: {}", notificationId, ex.getMessage());
            return PushDeliveryResult.failed(false, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String obtainAccessToken(ServiceAccountCredentials creds) throws Exception {
        long nowSec = Instant.now().getEpochSecond();
        CachedAccessToken current = this.cachedToken;
        if (current != null && current.isValid(nowSec)) {
            return current.token();
        }
        synchronized (this) {
            nowSec = Instant.now().getEpochSecond();
            if (this.cachedToken != null && this.cachedToken.isValid(nowSec)) {
                return this.cachedToken.token();
            }
            String signedJwt = createSignedServiceAccountJwt(creds, nowSec);
            String formBody = "grant_type="
                    + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                    + "&assertion="
                    + URLEncoder.encode(signedJwt, StandardCharsets.UTF_8);

            HttpRequest tokenReq = HttpRequest.newBuilder(URI.create(creds.tokenUri()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> tokenRes = httpClient.send(tokenReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (tokenRes.statusCode() < 200 || tokenRes.statusCode() >= 300) {
                throw new IllegalStateException("Google OAuth2 token exchange failed HTTP " + tokenRes.statusCode());
            }
            JsonNode json = objectMapper.readTree(tokenRes.body());
            String accessToken = json.path("access_token").asText("");
            long expiresIn = json.path("expires_in").asLong(3600L);
            if (accessToken.isBlank()) {
                throw new IllegalStateException("Google OAuth2 response missing access_token");
            }
            long refreshAfter = nowSec + Math.max(60L, expiresIn - 300L);
            this.cachedToken = new CachedAccessToken(accessToken, refreshAfter);
            return accessToken;
        }
    }

    private String createSignedServiceAccountJwt(ServiceAccountCredentials creds, long nowEpochSec) throws Exception {
        Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        byte[] headerBytes = objectMapper.writeValueAsBytes(Map.of("alg", "RS256", "typ", "JWT"));
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", creds.clientEmail());
        claims.put("scope", FCM_OAUTH_SCOPE);
        claims.put("aud", creds.tokenUri());
        claims.put("iat", nowEpochSec);
        claims.put("exp", nowEpochSec + 3600L);
        byte[] claimsBytes = objectMapper.writeValueAsBytes(claims);

        String signingInput = urlEncoder.encodeToString(headerBytes) + "." + urlEncoder.encodeToString(claimsBytes);
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(creds.privateKey());
        rsa.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        byte[] signatureBytes = rsa.sign();
        return signingInput + "." + urlEncoder.encodeToString(signatureBytes);
    }

    private ServiceAccountCredentials loadCredentialsSafely(String inlineJson, String configuredPath) {
        try {
            if (inlineJson != null && !inlineJson.isBlank()) {
                return parseServiceAccountJson(inlineJson, "FCM_CREDENTIALS_JSON");
            }
            if (configuredPath != null && !configuredPath.isBlank()) {
                Path p = Path.of(configuredPath.trim());
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    return parseServiceAccountJson(Files.readString(p, StandardCharsets.UTF_8), "FCM_CREDENTIALS_PATH");
                }
            }
            String googleEnvPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (googleEnvPath != null && !googleEnvPath.isBlank()) {
                Path envP = Path.of(googleEnvPath.trim());
                if (Files.exists(envP) && Files.isRegularFile(envP)) {
                    return parseServiceAccountJson(Files.readString(envP, StandardCharsets.UTF_8), "GOOGLE_APPLICATION_CREDENTIALS");
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to load Firebase Service Account credentials (falling back to in-app inbox): {}", ex.getMessage());
        }
        return null;
    }

    private ServiceAccountCredentials parseServiceAccountJson(String rawJson, String source) {
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            String projectId = node.path("project_id").asText("");
            String clientEmail = node.path("client_email").asText("");
            String privateKeyPem = node.path("private_key").asText("");
            String tokenUri = node.path("token_uri").asText(DEFAULT_TOKEN_URI);
            if (clientEmail.isBlank() || privateKeyPem.isBlank() || privateKeyPem.contains("REPLACE_WITH_")) {
                return null;
            }
            String normalizedPem = privateKeyPem
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalizedPem);
            PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            return new ServiceAccountCredentials(
                    projectId,
                    clientEmail,
                    pk,
                    tokenUri.isBlank() ? DEFAULT_TOKEN_URI : tokenUri,
                    source
            );
        } catch (Exception ex) {
            log.warn("Failed parsing Firebase Service Account JSON from {}: {}", source, ex.getMessage());
            return null;
        }
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) return "https://fcm.googleapis.com";
        String t = url.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }

    private static String safeStr(String val) {
        return val != null ? val : "";
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 3) return email;
        return email.substring(0, 3) + "***" + email.substring(at);
    }
}

