package com.jasper.resume.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "auth_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class AuthSession {
  @Id @Getter private String id = UUID.randomUUID().toString();

  @NonNull
  @Getter
  @Column(nullable = false)
  private String userId;

  @NonNull
  @Column(nullable = false, unique = true, length = 64)
  private String refreshTokenHash;

  @NonNull
  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  private Instant lastUsedAt = Instant.now();

  public void rotate(String hash, Instant expiry) {
    refreshTokenHash = hash;
    expiresAt = expiry;
    lastUsedAt = Instant.now();
  }

  public void revoke() {
    revoked = true;
  }

  public boolean active() {
    return !revoked && expiresAt.isAfter(Instant.now());
  }
}
