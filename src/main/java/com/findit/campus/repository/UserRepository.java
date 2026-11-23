package com.findit.campus.repository;

import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AppUser> findAllByOrderByCreatedAtDesc();
    long countByRole(UserRole role);
}
