package com.jasper.resume.dto.internal;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenPair {
  private String accessToken;
  private String refreshToken;
  private String sessionToken;
  private Duration accessLifetime;
  private Duration refreshLifetime;
}
