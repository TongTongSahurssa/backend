package com.jasper.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTests {
  private static final String EMAIL = "integration@example.com";
  private static final String PASSWORD = "strong-password";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registrationLoginRefreshAndLogoutWorkTogether() throws Exception {
    String registrationCode =
        developmentCode(
            postJson("/api/auth/register/request", Map.of("email", EMAIL, "password", PASSWORD)));

    postJson("/api/auth/register/verify", Map.of("email", EMAIL, "code", registrationCode))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Account created. You can now sign in."));

    String loginCode =
        developmentCode(
            postJson("/api/auth/login/request", Map.of("email", EMAIL, "password", PASSWORD)));

    MvcResult login =
        postJson("/api/auth/login/verify", Map.of("email", EMAIL, "code", loginCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(EMAIL))
            .andExpect(cookie().httpOnly("access_token", true))
            .andExpect(cookie().httpOnly("refresh_token", true))
            .andExpect(cookie().httpOnly("session_token", true))
            .andReturn();

    Map<String, Cookie> loginCookies = cookies(login);

    mockMvc
        .perform(
            get("/api/auth/me")
                .cookie(loginCookies.get("access_token"), loginCookies.get("session_token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value(EMAIL))
        .andExpect(jsonPath("$.data.role").value("ADMIN"));

    MvcResult refresh =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .cookie(
                        loginCookies.get("access_token"),
                        loginCookies.get("refresh_token"),
                        loginCookies.get("session_token")))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"))
            .andReturn();

    Map<String, Cookie> refreshedCookies = cookies(refresh);
    assertThat(refreshedCookies.get("refresh_token").getValue())
        .isNotEqualTo(loginCookies.get("refresh_token").getValue());

    mockMvc
        .perform(
            post("/api/auth/logout")
                .cookie(
                    refreshedCookies.get("access_token"), refreshedCookies.get("session_token")))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("access_token", 0))
        .andExpect(cookie().maxAge("refresh_token", 0))
        .andExpect(cookie().maxAge("session_token", 0));

    mockMvc
        .perform(
            get("/api/auth/me")
                .cookie(
                    refreshedCookies.get("access_token"), refreshedCookies.get("session_token")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointRejectsAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  private org.springframework.test.web.servlet.ResultActions postJson(String path, Object payload)
      throws Exception {
    return mockMvc.perform(
        post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(payload)));
  }

  private String developmentCode(org.springframework.test.web.servlet.ResultActions request)
      throws Exception {
    MvcResult result =
        request
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.developmentCode").isNotEmpty())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("developmentCode").asText();
  }

  private Map<String, Cookie> cookies(MvcResult result) {
    return Arrays.stream(result.getResponse().getCookies())
        .collect(Collectors.toMap(Cookie::getName, Function.identity()));
  }
}
