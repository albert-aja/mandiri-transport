package com.mandiritransport.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mandiritransport.ops.auth.JwtService;
import com.mandiritransport.ops.auth.entity.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    protected String loginAs(String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String supabaseBearer(String supabaseUid, String role) {
        AppUser synthetic = new AppUser();
        synthetic.setId(supabaseUid); // sub = supabase_uid to verify Supabase HS256 JWT lookup!
        synthetic.setUsername("supabase-user");
        synthetic.setFullName("Supabase User");
        synthetic.setRole(role);
        return "Bearer " + jwtService.generateToken(synthetic, null);
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected HttpHeaders authHeader(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.AUTHORIZATION, bearer(token));
        return h;
    }
}
