package com.findit.campus.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 40)
    private String nim;

    @Column(length = 120)
    private String studyProgram;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AppUser() {
    }

    public AppUser(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email.toLowerCase(Locale.ROOT);
        this.password = password;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getNim() { return nim; }
    public String getStudyProgram() { return studyProgram; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void updateProfile(String fullName, String nim, String studyProgram) {
        this.fullName = fullName.trim();
        this.nim = nim.trim();
        this.studyProgram = studyProgram.trim();
    }

    public void updateAccount(String fullName, String nim, String studyProgram, String email, UserRole role) {
        updateProfile(fullName, nim, studyProgram);
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.role = role;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return email; }
    @Override
    public String getPassword() { return password; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
