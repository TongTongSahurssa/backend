package com.jasper.resume.controller;

import com.jasper.resume.dto.response.ApiResponse;
import com.jasper.resume.dto.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
  @GetMapping("/health")
  public ResponseEntity<ApiResponse<HealthResponse>> health() {
    return ResponseEntity.ok(ApiResponse.success("Backend is healthy.", new HealthResponse("UP")));
  }
}
