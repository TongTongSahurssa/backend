package com.jasper.resume.config;

import com.jasper.resume.security.RestAuthenticationEntryPoint;
import com.jasper.resume.security.SessionValidationFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecretKey jwtKey(@Value("${app.auth.jwt-secret}") String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes.");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey key) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("resume-admin"));
    return decoder;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  BearerTokenResolver bearerTokenResolver() {
    return request -> {
      Cookie[] cookies = request.getCookies();
      if (cookies == null) return null;
      for (Cookie cookie : cookies)
        if ("access_token".equals(cookie.getName())) return cookie.getValue();
      return null;
    };
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      BearerTokenResolver resolver,
      SessionValidationFilter sessionFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/register/request",
                        "/api/auth/register/verify",
                        "/api/auth/login/request",
                        "/api/auth/login/verify",
                        "/api/auth/refresh",
                        "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(errors -> errors.authenticationEntryPoint(authenticationEntryPoint))
        .oauth2ResourceServer(
            resource ->
                resource
                    .jwt(jwt -> {})
                    .bearerTokenResolver(resolver)
                    .authenticationEntryPoint(authenticationEntryPoint))
        .addFilterAfter(
            sessionFilter,
            org.springframework.security.oauth2.server.resource.web.authentication
                .BearerTokenAuthenticationFilter.class)
        .build();
  }
}
