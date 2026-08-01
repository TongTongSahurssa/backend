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
@Table(name = "user_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class UserAccount {
  @Id @Getter private String id = UUID.randomUUID().toString();

  @NonNull
  @Getter
  @Column(nullable = false, unique = true)
  private String email;

  @NonNull
  @Getter
  @Column(nullable = false)
  private String passwordHash;

  @Getter
  @Column(nullable = false)
  private boolean verified;

  @Getter
  @Column(nullable = false)
  private String role = "ADMIN";

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public void verify() {
    verified = true;
  }
}
