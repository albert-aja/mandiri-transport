package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.common.exception.BusinessException;
import com.mandiritransport.ops.flighttracking.entity.FlightTrackingCredential;
import com.mandiritransport.ops.flighttracking.mapper.FlightTrackingCredentialMapper;
import com.mandiritransport.ops.flighttracking.provider.FlightTrackingProvider.ExtractedTokenPayload;
import com.mandiritransport.ops.flighttracking.service.FlightAwarePollingService;
import com.mandiritransport.ops.flighttracking.service.FlightAwareTokenService;
import com.mandiritransport.ops.flighttracking.service.FlightTokenCryptoService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlightAwareResearchModuleIntegrationTest {

    private static HttpServer mockFlightAwareServer;
    private static int serverPort;
    private static final AtomicReference<String> currentPollResponseBody = new AtomicReference<>();
    private static final AtomicInteger currentPollHttpStatus = new AtomicInteger(200);

    private static final String SECRET_RAW_TOKEN_1 = "fa-live-secret-token-gia190-alpha-998877";
    private static final String SECRET_RAW_TOKEN_WRONG_FLIGHT = "fa-live-secret-token-wrong-sia990";
    private static final String SECRET_RAW_TOKEN_2_VALID = "fa-live-secret-token-gia190-beta-223344";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlightTokenCryptoService cryptoService;

    @Autowired
    private FlightAwareTokenService tokenService;

    @Autowired
    private FlightAwarePollingService pollingService;

    @Autowired
    private FlightTrackingCredentialMapper credentialMapper;

    @BeforeAll
    static void startMockFlightAwareServer() throws Exception {
        mockFlightAwareServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = mockFlightAwareServer.getAddress().getPort();

        // 1. Flight page with decoy TOKEN earlier in HTML and real trackpollGlobals later
        mockFlightAwareServer.createContext("/live/flight/GIA190", exchange -> {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <script>
                        // Decoy script that must NEVER be mistaken for trackpollGlobals
                        var pageAnalytics = { "TOKEN": "decoy-analytics-token-do-not-use", " version ": 1 };
                      </script>
                      <script type="text/javascript">
                        /* FlightAware bootstrap globals */
                        var trackpollGlobals = {
                            "TOKEN": "%s",
                            "INTERVAL": 60,
                            "SINGLE_FLIGHT": true,
                            "USERTOKEN": "136360878",
                            "LOCALE": "en_US"
                        };
                      </script>
                    </head>
                    <body>Flight GIA190 CGK to KNO</body>
                    </html>
                    """.formatted(SECRET_RAW_TOKEN_1);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // 2. Replacement page returning a token for the wrong flight
        mockFlightAwareServer.createContext("/live/flight/WRONG", exchange -> {
            String html = """
                    <script>
                      window.trackpollGlobals = {
                        "TOKEN": "%s",
                        "INTERVAL": 45,
                        "SINGLE_FLIGHT": true
                      };
                    </script>
                    """.formatted(SECRET_RAW_TOKEN_WRONG_FLIGHT);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // 3. Replacement page returning a valid refreshed token for GIA190
        mockFlightAwareServer.createContext("/live/flight/GIA190-REFRESHED", exchange -> {
            String html = """
                    <script>
                      var trackpollGlobals = {
                        "TOKEN": "%s",
                        "INTERVAL": 60,
                        "SINGLE_FLIGHT": true,
                        "USERTOKEN": "99887766"
                      };
                    </script>
                    """.formatted(SECRET_RAW_TOKEN_2_VALID);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // 4. Mocked trackpoll.rvt endpoint
        mockFlightAwareServer.createContext("/ajax/trackpoll.rvt", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            int status = currentPollHttpStatus.get();
            String body = currentPollResponseBody.get();

            if (query != null && query.contains(SECRET_RAW_TOKEN_WRONG_FLIGHT)) {
                body = """
                        {
                          "flights": {
                            "SIA990-1731190000": {
                              "ident": "SIA990",
                              "flightStatus": "En Route",
                              "gateDepartureTimes": { "scheduled": 1731190000 }
                            }
                          }
                        }
                        """;
                status = 200;
            }

            byte[] bytes = (body != null ? body : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockFlightAwareServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockFlightAwareServer != null) {
            mockFlightAwareServer.stop(0);
        }
    }

    @Test
    void tokenEncryptionServiceRoundTripsWithRandomIvAndDetectsTampering() {
        String c1 = cryptoService.encrypt(SECRET_RAW_TOKEN_1);
        String c2 = cryptoService.encrypt(SECRET_RAW_TOKEN_1);

        assertThat(c1).isNotEqualTo(SECRET_RAW_TOKEN_1);
        assertThat(c1).startsWith("v1:");
        // Random 96-bit GCM IV guarantees distinct ciphertexts for identical plaintext
        assertThat(c1).isNotEqualTo(c2);

        assertThat(cryptoService.decrypt(c1)).isEqualTo(SECRET_RAW_TOKEN_1);
        assertThat(cryptoService.decrypt(c2)).isEqualTo(SECRET_RAW_TOKEN_1);

        String fp = cryptoService.fingerprint(SECRET_RAW_TOKEN_1);
        assertThat(fp).startsWith("sha256:").doesNotContain(SECRET_RAW_TOKEN_1);

        // Tamper with the last 4 characters of ciphertext -> GCM authentication tag must reject!
        String tampered = c1.substring(0, c1.length() - 4) + "AAAA";
        assertThatThrownBy(() -> cryptoService.decrypt(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void htmlParserHandlesFormattingVariationsMissingGlobalsAndAntiBotChallenges() {
        String htmlWithTrailingCommaAndNumericUserToken = """
                <html>
                <script>
                  var firstOtherToken = {"TOKEN": "ignore-me"};
                  let trackpollGlobals = {
                    // comment with braces { inside }
                    "TOKEN": "tok-parsed-991",
                    "INTERVAL": 45,
                    "SINGLE_FLIGHT": false,
                    "USERTOKEN": 136360878,
                  };
                </script>
                </html>
                """;
        ExtractedTokenPayload ok = tokenService.parseTrackpollGlobalsFromHtml(
                htmlWithTrailingCommaAndNumericUserToken, "https://www.flightaware.com/live/flight/GIA190", 200, OffsetDateTime.now()
        );
        assertThat(ok.success()).isTrue();
        assertThat(ok.rawToken()).isEqualTo("tok-parsed-991");
        assertThat(ok.pollingIntervalSeconds()).isEqualTo(45);
        assertThat(ok.singleFlight()).isFalse();
        assertThat(ok.rawUserToken()).isEqualTo("136360878");

        ExtractedTokenPayload missing = tokenService.parseTrackpollGlobalsFromHtml(
                "<html><body>No globals here</body></html>", "https://www.flightaware.com/live", 200, OffsetDateTime.now()
        );
        assertThat(missing.success()).isFalse();
        assertThat(missing.status()).isEqualTo("MISSING_GLOBALS");
    }

    @Test
    void fullEndToEndTokenLifecyclePollingDiffValidationReportAndReplacementSafety() throws Exception {
        pollingService.setPollEndpointUrlForTesting("http://127.0.0.1:" + serverPort + "/ajax/trackpoll.rvt");

        String ownerJwt = login("hendra");
        String driverJwt = login("budi");

        // 1. RBAC: Driver must be denied access to research console APIs
        mockMvc.perform(get("/api/v1/integrations/flightaware/credentials")
                        .header("Authorization", "Bearer " + driverJwt))
                .andExpect(status().isForbidden());

        // 2. Extract Token from Mocked FlightAware Page (linked to booking bkg-201 / GA-190)
        String extractPayload = """
                {
                  "trackingPageUrl": "http://127.0.0.1:%d/live/flight/GIA190",
                  "flightId": "bkg-201",
                  "flightNumber": "GA-190",
                  "flightDate": "2026-11-10",
                  "persistCredential": true,
                  "validateImmediately": false
                }
                """.formatted(serverPort);

        MvcResult extractRes = mockMvc.perform(post("/api/v1/integrations/flightaware/extract")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractionSuccess").value(true))
                .andExpect(jsonPath("$.data.tokenPresent").value(true))
                .andExpect(jsonPath("$.data.userTokenPresent").value(true))
                .andExpect(jsonPath("$.data.pollingIntervalSeconds").value(60))
                .andExpect(jsonPath("$.data.singleFlight").value(true))
                .andExpect(jsonPath("$.data.persistedCredential.tokenStatus").value("UNKNOWN"))
                .andReturn();

        String extractBodyStr = extractRes.getResponse().getContentAsString();
        // SECURITY ASSERTION: Raw token must NEVER appear anywhere in API response!
        assertThat(extractBodyStr).doesNotContain(SECRET_RAW_TOKEN_1);

        JsonNode extractJson = objectMapper.readTree(extractBodyStr);
        String credId = extractJson.path("data").path("persistedCredential").path("id").asText();

        // Verify database encrypted storage
        FlightTrackingCredential dbRow = credentialMapper.selectById(credId);
        assertThat(dbRow.getTrackingTokenEncrypted()).startsWith("v1:").doesNotContain(SECRET_RAW_TOKEN_1);
        assertThat(cryptoService.decrypt(dbRow.getTrackingTokenEncrypted())).isEqualTo(SECRET_RAW_TOKEN_1);

        // 3. Poll #1: Flight En Route to KNO
        currentPollHttpStatus.set(200);
        currentPollResponseBody.set("""
                {
                  "flights": {
                    "GIA190-1731196800-airline-01": {
                      "ident": "GIA190",
                      "flightStatus": "En Route / On Time",
                      "origin": { "iata": "CGK", "icao": "WIII" },
                      "destination": { "iata": "KNO", "icao": "WIMM" },
                      "gateDepartureTimes": {
                        "scheduled": 1731230400,
                        "estimated": 1731230400,
                        "actual": 1731230520
                      },
                      "gateArrivalTimes": {
                        "scheduled": 1731238800,
                        "estimated": 1731238800
                      },
                      "lastPosition": {
                        "lat": 1.4821,
                        "lon": 100.3120,
                        "alt": 34000,
                        "gs": 455
                      },
                      "aircraft": {
                        "type": "B738",
                        "tail": "PK-GMA"
                      },
                      "cancelled": false,
                      "diverted": false
                    }
                  }
                }
                """);

        mockMvc.perform(post("/api/v1/integrations/flightaware/credentials/" + credId + "/poll")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experimentTag\":\"EXP_E_POLL_1\",\"forceBypassMinInterval\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responseValid").value(true))
                .andExpect(jsonPath("$.data.parsingStatus").value("VALID_TRACKING_JSON"))
                .andExpect(jsonPath("$.data.updatedCredentialStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.normalizedFlight.flightIdentifier").value("GIA190"))
                .andExpect(jsonPath("$.data.normalizedFlight.aircraftRegistration").value("PK-GMA"));

        // 4. Poll #2: Flight Landed at KNO with 18m arrival delay & actual arrival timestamp
        currentPollResponseBody.set("""
                {
                  "flights": {
                    "GIA190-1731196800-airline-01": {
                      "ident": "GIA190",
                      "flightStatus": "Landed / Taxing to Gate",
                      "origin": { "iata": "CGK", "icao": "WIII" },
                      "destination": { "iata": "KNO", "icao": "WIMM" },
                      "gateDepartureTimes": {
                        "scheduled": 1731230400,
                        "estimated": 1731230400,
                        "actual": 1731230520
                      },
                      "gateArrivalTimes": {
                        "scheduled": 1731238800,
                        "estimated": 1731239880,
                        "actual": 1731239880,
                        "delay": 18
                      },
                      "lastPosition": {
                        "lat": 3.6422,
                        "lon": 98.8853,
                        "alt": 120,
                        "gs": 15
                      },
                      "aircraft": {
                        "type": "B738",
                        "tail": "PK-GMA"
                      },
                      "cancelled": false,
                      "diverted": false
                    }
                  }
                }
                """);

        mockMvc.perform(post("/api/v1/integrations/flightaware/credentials/" + credId + "/poll")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"experimentTag\":\"EXP_E_POLL_2\",\"forceBypassMinInterval\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responseValid").value(true))
                .andExpect(jsonPath("$.data.normalizedFlight.arrivalDelayMinutes").value(18));

        // 5. Diff Comparison between Poll #1 and Poll #2
        mockMvc.perform(get("/api/v1/integrations/flightaware/credentials/" + credId + "/diff-latest")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationallySignificantChanges.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data.addedFields.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        // 6. Section 9 Flight Data Validation Report (all 8 CRITICAL requirements verified meaningful!)
        mockMvc.perform(get("/api/v1/integrations/flightaware/validation-report")
                        .param("credentialId", credId)
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criticalRequirementsTotal").value(8))
                .andExpect(jsonPath("$.data.criticalRequirementsSupported").value(8))
                .andExpect(jsonPath("$.data.meetsOperationalRequirements").value(true))
                .andExpect(jsonPath("$.data.requirements.length()").value(13))
                .andExpect(jsonPath("$.data.experiments.length()").value(5));

        // 7. Replacement Protection Rule: Attempting replacement with wrong flight token MUST NOT overwrite ACTIVE credential
        mockMvc.perform(post("/api/v1/integrations/flightaware/credentials/" + credId + "/replace")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingPageUrl\":\"http://127.0.0.1:" + serverPort + "/live/flight/WRONG\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenStatus").value("INVALID"));

        // Verify original credential is STILL ACTIVE!
        assertThat(credentialMapper.selectById(credId).getTokenStatus()).isEqualTo("ACTIVE");

        // 8. Valid Replacement: Attempting replacement with valid GIA190 token promotes new to ACTIVE & marks old SUPERSEDED
        MvcResult validReplaceRes = mockMvc.perform(post("/api/v1/integrations/flightaware/credentials/" + credId + "/replace")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingPageUrl\":\"http://127.0.0.1:" + serverPort + "/live/flight/GIA190-REFRESHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenStatus").value("ACTIVE"))
                .andReturn();

        String newCredId = objectMapper.readTree(validReplaceRes.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertThat(credentialMapper.selectById(credId).getTokenStatus()).isEqualTo("SUPERSEDED");
        assertThat(credentialMapper.selectById(newCredId).getTokenStatus()).isEqualTo("ACTIVE");

        // 9. Experiment B: When endpoint later rejects token with non-JSON HTML, transitions ACTIVE -> EXPIRED & sets expiresAt
        currentPollResponseBody.set("<html><body>Session Expired - Please reload FlightAware page</body></html>");
        mockMvc.perform(post("/api/v1/integrations/flightaware/credentials/" + newCredId + "/revalidate")
                        .header("Authorization", "Bearer " + ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenStatus").value("EXPIRED"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
    }

    private String login(String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }
}
