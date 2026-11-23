package com.findit.campus.service;

import com.findit.campus.entity.AppUser;
import com.findit.campus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AccountService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(String fullName, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email tersebut sudah terdaftar.");
        }
        return users.save(new AppUser(fullName.trim(), normalizedEmail, passwordEncoder.encode(password)));
    }
}
