package com.jasper.resume.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
  private String id;
  private String email;
  private String role;
}
