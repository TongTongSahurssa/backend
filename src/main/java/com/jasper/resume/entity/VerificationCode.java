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
import lombok.Setter;

@Entity
@Table(name = "auth_challenges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class VerificationCode {
  @Id private String id = UUID.randomUUID().toString();

  @NonNull
  @Column(nullable = false)
  private String email;

  @NonNull
  @Column(nullable = false)
  private String purpose;

  @NonNull
  @Getter
  @Column(nullable = false, length = 64)
  private String codeHash;

  @Getter @Setter private String pendingPasswordHash;

  @Column(nullable = false)
  private Instant expiresAt = Instant.now().plusSeconds(600);

  @Column(nullable = false)
  private boolean used;

  @Column(nullable = false)
  private int attempts;

  public boolean valid() {
    return !used && attempts < 5 && expiresAt.isAfter(Instant.now());
  }

  public void failedAttempt() {
    attempts++;
  }

  public void consume() {
    used = true;
  }
}
