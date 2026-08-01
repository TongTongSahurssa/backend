package com.jasper.resume.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {
  @NotBlank @Email private String email;

  @NotBlank
  @Pattern(regexp = "\\d{6}", message = "Verification code must contain 6 digits")
  private String code;
}
