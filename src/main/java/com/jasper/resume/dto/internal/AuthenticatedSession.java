package com.jasper.resume.dto.internal;

import com.jasper.resume.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticatedSession {
  private TokenPair tokens;
  private UserResponse user;
}
