package com.jasper.resume.repository;

import com.jasper.resume.entity.AuthSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<AuthSession, String> {
  Optional<AuthSession> findByRefreshTokenHash(String hash);
}
