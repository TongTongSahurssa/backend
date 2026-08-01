package com.jasper.resume.dto.response;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {
  private boolean success;
  private String message;
  private Map<String, String> errors;
  private Instant timestamp;

  public static ApiErrorResponse of(String message) {
    return new ApiErrorResponse(false, message, Map.of(), Instant.now());
  }

  public static ApiErrorResponse validation(String message, Map<String, String> errors) {
    return new ApiErrorResponse(false, message, Map.copyOf(errors), Instant.now());
  }
}
