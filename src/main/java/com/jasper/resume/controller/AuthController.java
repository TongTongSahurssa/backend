package com.jasper.resume.controller;

import com.jasper.resume.dto.internal.AuthenticatedSession;
import com.jasper.resume.dto.internal.TokenPair;
import com.jasper.resume.dto.request.CredentialsRequest;
import com.jasper.resume.dto.request.VerificationRequest;
import com.jasper.resume.dto.response.ApiResponse;
import com.jasper.resume.dto.response.UserResponse;
import com.jasper.resume.dto.response.VerificationResponse;
import com.jasper.resume.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  private final boolean secure;

  public AuthController(AuthService auth, @Value("${app.auth.secure-cookies}") boolean secure) {
    this.auth = auth;
    this.secure = secure;
  }

  @PostMapping("/register/request")
  public ResponseEntity<ApiResponse<VerificationResponse>> requestRegistration(
      @Valid @RequestBody CredentialsRequest body) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Verification code sent.",
            auth.requestRegistration(body.getEmail(), body.getPassword())));
  }

  @PostMapping("/register/verify")
  public ResponseEntity<ApiResponse<Void>> verifyRegistration(
      @Valid @RequestBody VerificationRequest body) {
    auth.verifyRegistration(body.getEmail(), body.getCode());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Account created. You can now sign in."));
  }

  @PostMapping("/login/request")
  public ResponseEntity<ApiResponse<VerificationResponse>> requestLogin(
      @Valid @RequestBody CredentialsRequest body) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Verification code sent.", auth.requestLogin(body.getEmail(), body.getPassword())));
  }

  @PostMapping("/login/verify")
  public ResponseEntity<ApiResponse<UserResponse>> verifyLogin(
      @Valid @RequestBody VerificationRequest body, HttpServletResponse response) {
    AuthenticatedSession session = auth.verifyLogin(body.getEmail(), body.getCode());
    writeCookies(response, session.getTokens());
    return ResponseEntity.ok(ApiResponse.success("Signed in.", session.getUser()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<UserResponse>> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    AuthenticatedSession session =
        auth.refresh(cookie(request, "refresh_token"), cookie(request, "session_token"));
    writeCookies(response, session.getTokens());
    return ResponseEntity.ok(ApiResponse.success("Session refreshed.", session.getUser()));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      HttpServletRequest request, HttpServletResponse response) {
    auth.logout(cookie(request, "session_token"));
    clearCookies(response);
    return ResponseEntity.ok(ApiResponse.success("Signed out."));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> me(JwtAuthenticationToken token) {
    return ResponseEntity.ok(
        ApiResponse.success("Current user loaded.", auth.current(token.getToken().getSubject())));
  }

  private void writeCookies(HttpServletResponse response, TokenPair pair) {
    addCookie(
        response, "access_token", pair.getAccessToken(), pair.getAccessLifetime().toSeconds());
    addCookie(
        response, "refresh_token", pair.getRefreshToken(), pair.getRefreshLifetime().toSeconds());
    addCookie(
        response, "session_token", pair.getSessionToken(), pair.getRefreshLifetime().toSeconds());
  }

  private void clearCookies(HttpServletResponse response) {
    addCookie(response, "access_token", "", 0);
    addCookie(response, "refresh_token", "", 0);
    addCookie(response, "session_token", "", 0);
  }

  private void addCookie(HttpServletResponse response, String name, String value, long age) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/")
            .maxAge(age)
            .build()
            .toString());
  }

  private String cookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies())
      if (name.equals(cookie.getName())) return cookie.getValue();
    return null;
  }
}
