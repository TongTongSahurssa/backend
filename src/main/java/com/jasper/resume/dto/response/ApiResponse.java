package com.jasper.resume.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
  private boolean success;
  private String message;
  private T data;
  private Instant timestamp;

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data, Instant.now());
  }

  public static ApiResponse<Void> success(String message) {
    return success(message, null);
  }
}
