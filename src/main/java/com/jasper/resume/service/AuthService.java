package com.jasper.resume.service;

import com.jasper.resume.dto.internal.AuthenticatedSession;
import com.jasper.resume.dto.internal.TokenPair;
import com.jasper.resume.dto.response.UserResponse;
import com.jasper.resume.dto.response.VerificationResponse;
import com.jasper.resume.entity.AuthSession;
import com.jasper.resume.entity.UserAccount;
import com.jasper.resume.entity.VerificationCode;
import com.jasper.resume.exception.AuthException;
import com.jasper.resume.repository.SessionRepository;
import com.jasper.resume.repository.UserRepository;
import com.jasper.resume.repository.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthService {
  private final UserRepository users;
  private final SessionRepository sessions;
  private final VerificationCodeRepository codes;
  private final PasswordEncoder passwords;
  private final JwtEncoder jwtEncoder;
  private final JavaMailSender mailSender;
  private final SecureRandom random = new SecureRandom();
  private final Duration accessLifetime;
  private final Duration refreshLifetime;
  private final boolean developmentMail;
  private final String mailFrom;

  public AuthService(
      UserRepository users,
      SessionRepository sessions,
      VerificationCodeRepository codes,
      PasswordEncoder passwords,
      JwtEncoder jwtEncoder,
      ObjectProvider<JavaMailSender> mailSender,
      @Value("${app.auth.access-token-minutes}") long accessMinutes,
      @Value("${app.auth.refresh-token-days}") long refreshDays,
      @Value("${app.mail.development-mode}") boolean developmentMail,
      @Value("${app.mail.from}") String mailFrom) {
    this.users = users;
    this.sessions = sessions;
    this.codes = codes;
    this.passwords = passwords;
    this.jwtEncoder = jwtEncoder;
    this.mailSender = mailSender.getIfAvailable();
    this.accessLifetime = Duration.ofMinutes(accessMinutes);
    this.refreshLifetime = Duration.ofDays(refreshDays);
    this.developmentMail = developmentMail;
    this.mailFrom = mailFrom;
  }

  public VerificationResponse requestRegistration(String email, String password) {
    String normalized = normalizeEmail(email);
    if (password == null || password.length() < 10)
      throw new AuthException("Password must be at least 10 characters.");
    if (users.findByEmailIgnoreCase(normalized).isPresent())
      throw new AuthException("An account already exists for this email.");
    return sendVerification(normalized, "REGISTER", passwords.encode(password));
  }

  public void verifyRegistration(String email, String code) {
    String normalized = normalizeEmail(email);
    if (users.findByEmailIgnoreCase(normalized).isPresent())
      throw new AuthException("An account already exists for this email.");
    VerificationCode challenge = verifyChallenge(normalized, "REGISTER", code);
    UserAccount user = new UserAccount(normalized, challenge.getPendingPasswordHash());
    user.verify();
    users.save(user);
  }

  public VerificationResponse requestLogin(String email, String password) {
    UserAccount user =
        users
            .findByEmailIgnoreCase(normalizeEmail(email))
            .orElseThrow(() -> new AuthException("Invalid email or password."));
    if (!passwords.matches(password, user.getPasswordHash()))
      throw new AuthException("Invalid email or password.");
    if (!user.isVerified()) throw new AuthException("Verify your email before signing in.");
    return sendVerification(user.getEmail(), "LOGIN", null);
  }

  public AuthenticatedSession verifyLogin(String email, String code) {
    UserAccount user =
        users
            .findByEmailIgnoreCase(normalizeEmail(email))
            .orElseThrow(() -> new AuthException("Account not found."));
    verifyChallenge(user.getEmail(), "LOGIN", code);
    String refresh = opaqueToken();
    AuthSession session =
        sessions.save(
            new AuthSession(user.getId(), hash(refresh), Instant.now().plus(refreshLifetime)));
    return authenticatedSession(user, session, refresh);
  }

  public AuthenticatedSession refresh(String refreshToken, String sessionToken) {
    if (refreshToken == null || sessionToken == null)
      throw new AuthException("Refresh session is missing.");
    AuthSession session =
        sessions
            .findByRefreshTokenHash(hash(refreshToken))
            .orElseThrow(() -> new AuthException("Refresh token is invalid."));
    if (!session.active() || !constantEquals(session.getId(), sessionToken))
      throw new AuthException("Session expired or revoked.");
    UserAccount user =
        users
            .findById(session.getUserId())
            .orElseThrow(() -> new AuthException("Account not found."));
    String rotated = opaqueToken();
    session.rotate(hash(rotated), Instant.now().plus(refreshLifetime));
    return authenticatedSession(user, session, rotated);
  }

  public void logout(String sessionToken) {
    if (sessionToken != null) sessions.findById(sessionToken).ifPresent(AuthSession::revoke);
  }

  public UserResponse current(String userId) {
    UserAccount user =
        users.findById(userId).orElseThrow(() -> new AuthException("Account not found."));
    return toUserResponse(user);
  }

  private VerificationResponse sendVerification(
      String email, String purpose, String pendingPasswordHash) {
    String code = "%06d".formatted(random.nextInt(1_000_000));
    VerificationCode verificationCode = new VerificationCode(email, purpose, hash(code));
    verificationCode.setPendingPasswordHash(pendingPasswordHash);
    codes.save(verificationCode);
    if (!developmentMail) {
      if (mailSender == null || mailFrom == null || mailFrom.isBlank())
        throw new AuthException("SMTP sender credentials are not configured.");
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(mailFrom);
      message.setTo(email);
      message.setSubject(
          "Your " + ("LOGIN".equals(purpose) ? "login" : "registration") + " verification code");
      message.setText(
          "Your verification code is "
              + code
              + ". It expires in 10 minutes. If you did not request this, ignore this email.");
      try {
        mailSender.send(message);
      } catch (MailException exception) {
        throw new AuthException(
            "Verification email could not be delivered. Check the SMTP settings.");
      }
    }
    return new VerificationResponse(developmentMail ? code : null);
  }

  private VerificationCode verifyChallenge(String email, String purpose, String code) {
    VerificationCode candidate =
        codes.findByEmailIgnoreCaseAndPurposeOrderByExpiresAtDesc(email, purpose).stream()
            .findFirst()
            .orElseThrow(() -> new AuthException("Request a new verification code."));
    if (!candidate.valid()) throw new AuthException("Verification code expired or locked.");
    if (!constantEquals(candidate.getCodeHash(), hash(code))) {
      candidate.failedAttempt();
      throw new AuthException("Incorrect verification code.");
    }
    candidate.consume();
    return candidate;
  }

  private TokenPair tokens(UserAccount user, AuthSession session, String refreshToken) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("resume-admin")
            .issuedAt(now)
            .expiresAt(now.plus(accessLifetime))
            .subject(user.getId())
            .id(UUID.randomUUID().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole())
            .claim("sid", session.getId())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    String access = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenPair(access, refreshToken, session.getId(), accessLifetime, refreshLifetime);
  }

  private AuthenticatedSession authenticatedSession(
      UserAccount user, AuthSession session, String refreshToken) {
    return new AuthenticatedSession(tokens(user, session, refreshToken), toUserResponse(user));
  }

  private UserResponse toUserResponse(UserAccount user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getRole());
  }

  private String opaqueToken() {
    byte[] value = new byte[32];
    random.nextBytes(value);
    return HexFormat.of().formatHex(value);
  }

  private String normalizeEmail(String email) {
    if (email == null || !email.contains("@")) throw new AuthException("Enter a valid email.");
    return email.trim().toLowerCase();
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private boolean constantEquals(String left, String right) {
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }
}
