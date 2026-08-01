package com.jasper.resume.repository;

import com.jasper.resume.entity.VerificationCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, String> {
  List<VerificationCode> findByEmailIgnoreCaseAndPurposeOrderByExpiresAtDesc(
      String email, String purpose);
}
