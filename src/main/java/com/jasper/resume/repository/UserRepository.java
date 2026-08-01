package com.jasper.resume.repository;

import com.jasper.resume.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, String> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);
}
