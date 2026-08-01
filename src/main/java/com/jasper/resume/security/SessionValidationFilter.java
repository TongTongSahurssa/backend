package com.jasper.resume.security;

import com.jasper.resume.entity.AuthSession;
import com.jasper.resume.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionValidationFilter extends OncePerRequestFilter {
  private final SessionRepository sessions;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;

  public SessionValidationFilter(
      SessionRepository sessions, RestAuthenticationEntryPoint authenticationEntryPoint) {
    this.sessions = sessions;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication()
        instanceof JwtAuthenticationToken auth) {
      String jwtSession = auth.getToken().getClaimAsString("sid");
      String cookieSession = cookie(request, "session_token");
      boolean valid =
          jwtSession != null
              && jwtSession.equals(cookieSession)
              && sessions.findById(jwtSession).map(AuthSession::active).orElse(false);
      if (!valid) {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
            request,
            response,
            new InsufficientAuthenticationException("Session is invalid or revoked."));
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private String cookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies())
      if (name.equals(cookie.getName())) return cookie.getValue();
    return null;
  }
}
