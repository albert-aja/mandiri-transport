package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.notification.service.FirebaseHttpV1PushGateway;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveFcmPushAndRemindersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FirebaseHttpV1PushGateway fcmGateway;

    private HttpServer mockGoogleServer;
    private PublicKey rsaPublicKey;
    private final AtomicInteger oauthTokenCalls = new AtomicInteger(0);
    private final List<JsonNode> receivedFcmPayloads = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUpMockFcmServer() throws Exception {
        oauthTokenCalls.set(0);
        receivedFcmPayloads.clear();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        this.rsaPublicKey = kp.getPublic();

        mockGoogleServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = mockGoogleServer.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port;

        mockGoogleServer.createContext("/oauth2/token", exchange -> {
            String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String assertion = extractFormParam(form, "assertion");
            boolean sigValid = verifyRs256JwtSignature(assertion, rsaPublicKey);
            oauthTokenCalls.incrementAndGet();

            byte[] resp;
            int code;
            if (sigValid) {
                code = 200;
                resp = "{\"access_token\":\"live-fcm-oauth-access-token\",\"expires_in\":3600}"
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                code = 401;
                resp = "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        mockGoogleServer.createContext("/v1/projects/mandiri-transport-ops/messages:send", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            String rawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode payload = objectMapper.readTree(rawBody);
            receivedFcmPayloads.add(payload);

            String targetToken = payload.path("message").path("token").asText("");
            byte[] resp;
            int code;
            if (!"Bearer live-fcm-oauth-access-token".equals(auth)) {
                code = 401;
                resp = "{\"error\":\"unauthenticated\"}".getBytes(StandardCharsets.UTF_8);
            } else if (targetToken.contains("uninstalled")) {
                code = 404;
                resp = "{\"error\":{\"code\":404,\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}"
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                code = 200;
                resp = "{\"name\":\"projects/mandiri-transport-ops/messages/msg-live-001\"}"
                        .getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        mockGoogleServer.start();

        String pemKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        String serviceAccountJson = objectMapper.writeValueAsString(Map.of(
                "type", "service_account",
                "project_id", "mandiri-transport-ops",
                "client_email", "firebase-adminsdk-live@mandiri-transport-ops.iam.gserviceaccount.com",
                "private_key", pemKey,
                "token_uri", baseUrl + "/oauth2/token"
        ));

        fcmGateway.reconfigureForRuntime(true, serviceAccountJson, baseUrl);
    }

    @AfterEach
    void tearDown() {
        fcmGateway.reconfigureForRuntime(true, "", "https://fcm.googleapis.com");
        if (mockGoogleServer != null) {
            mockGoogleServer.stop(0);
        }
    }

    @Test
    void liveFcmHttpV1DeliversMultiDevicePushesPrunesUnregisteredTokensAndScansReminders() throws Exception {
        String ownerJwt = login("hendra");
        String budiJwt = login("budi");

        // 1. Register 2 devices for Owner (Phone + Garage Tablet) + 1 stale uninstalled token
        mockMvc.perform(post("/api/v1/auth/fcm-token")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fcmToken", "fcm-live-owner-phone-01",
                                "platform", "ANDROID",
                                "deviceLabel", "Samsung HP Pak Hendra"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(true));

        mockMvc.perform(post("/api/v1/auth/fcm-token")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fcmToken", "fcm-live-owner-tablet-02",
                                "platform", "ANDROID",
                                "deviceLabel", "Tablet Garasi Medan"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/fcm-token")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fcmToken", "fcm-stale-uninstalled-03",
                                "platform", "ANDROID",
                                "deviceLabel", "HP Lama"
                        ))))
                .andExpect(status().isOk());

        // Verify /fcm-status shows live credentials ready and 3 active devices before push
        mockMvc.perform(get("/api/v1/notifications/fcm-status")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liveCredentialsReady").value(true))
                .andExpect(jsonPath("$.data.myActiveDevicesCount").value(3));

        // 2. Trigger Live Test Push -> delivers to 2 valid devices, prunes 1 UNREGISTERED device, caches OAuth2 token
        mockMvc.perform(post("/api/v1/notifications/test-push")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Uji Live FCM Medan",
                                "body", "Koneksi HTTP v1 aktif."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fcmDeliveryStatus").value("PUSH_SENT"))
                .andExpect(jsonPath("$.data.devicesTargeted").value(3))
                .andExpect(jsonPath("$.data.devicesDelivered").value(2))
                .andExpect(jsonPath("$.data.fcmMessageId").value("projects/mandiri-transport-ops/messages/msg-live-001"));

        assertThat(oauthTokenCalls.get()).isEqualTo(1);
        assertThat(receivedFcmPayloads).hasSize(3);
        assertThat(receivedFcmPayloads.getFirst().path("message").path("android").path("notification").path("channel_id").asText())
                .isEqualTo("mandiri_ops_high_importance");

        // Stale UNREGISTERED token was automatically deactivated -> active count is now 2
        mockMvc.perform(get("/api/v1/notifications/fcm-status")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myActiveDevicesCount").value(2));

        // 3. Shared fleet phone switch: if Budi logs in on Tablet Garasi, token rebinds cleanly; unregister on logout
        mockMvc.perform(post("/api/v1/auth/fcm-token")
                        .header("Authorization", "Bearer " + budiJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fcmToken", "fcm-live-owner-tablet-02",
                                "platform", "ANDROID",
                                "deviceLabel", "HP Operasional Supir"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/fcm-status")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myActiveDevicesCount").value(1));

        mockMvc.perform(post("/api/v1/auth/fcm-token/unregister")
                        .header("Authorization", "Bearer " + budiJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fcmToken", "fcm-live-owner-tablet-02"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unregistered").value(true));

        // 4. Operational Reminder Scan dispatches live FCM alerts for expiring STNK/KIR & aged driver cash (deduplicated)
        MvcResult firstScan = mockMvc.perform(post("/api/v1/notifications/scan-reminders")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andReturn();
        int generatedFirst = objectMapper.readTree(firstScan.getResponse().getContentAsString())
                .path("data").path("generatedCount").asInt();
        assertThat(generatedFirst).isGreaterThanOrEqualTo(2);

        // Immediate second scan must produce 0 duplicates
        mockMvc.perform(post("/api/v1/notifications/scan-reminders")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(0));
        assertThat(oauthTokenCalls.get()).isEqualTo(1);
    }

    private String login(String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    private static String extractFormParam(String body, String key) {
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static boolean verifyRs256JwtSignature(String jwt, PublicKey publicKey) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) return false;
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            byte[] sigBytes = Base64.getUrlDecoder().decode(parts[2]);
            return sig.verify(sigBytes);
        } catch (Exception ex) {
            return false;
        }
    }
}
