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

    @Transactional
    public void updateProfile(AppUser currentUser, String fullName, String nim, String studyProgram) {
        String normalizedNim = nim.trim();
        if (users.existsByNimIgnoreCaseAndIdNot(normalizedNim, currentUser.getId())) {
            throw new IllegalArgumentException("NIM tersebut sudah digunakan akun lain.");
        }
        AppUser persistedUser = users.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Akun tidak ditemukan."));
        persistedUser.updateProfile(fullName, normalizedNim, studyProgram);
        currentUser.updateProfile(fullName, normalizedNim, studyProgram);
    }
}
