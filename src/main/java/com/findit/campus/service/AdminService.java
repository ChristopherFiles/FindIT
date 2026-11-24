package com.findit.campus.service;

import com.findit.campus.dto.AdminUserForm;
import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.ClaimStatus;
import com.findit.campus.entity.ItemClaim;
import com.findit.campus.entity.LostFoundReport;
import com.findit.campus.entity.ReportComment;
import com.findit.campus.entity.ReportStatus;
import com.findit.campus.entity.UserRole;
import com.findit.campus.repository.ClaimRepository;
import com.findit.campus.repository.NotificationRepository;
import com.findit.campus.repository.ReportCommentRepository;
import com.findit.campus.repository.ReportRepository;
import com.findit.campus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository users;
    private final ReportRepository reports;
    private final ClaimRepository claims;
    private final ReportCommentRepository comments;
    private final NotificationRepository notifications;
    private final UploadStorage uploadStorage;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository users, ReportRepository reports, ClaimRepository claims,
                        ReportCommentRepository comments, NotificationRepository notifications,
                        UploadStorage uploadStorage, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.reports = reports;
        this.claims = claims;
        this.comments = comments;
        this.notifications = notifications;
        this.uploadStorage = uploadStorage;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AppUser> users() {
        return users.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<LostFoundReport> reports() {
        return reports.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public AppUser user(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
    }

    @Transactional(readOnly = true)
    public long userCount() {
        return users.count();
    }

    @Transactional(readOnly = true)
    public long pendingClaimCount() {
        return claims.countByStatus(ClaimStatus.PENDING);
    }

    @Transactional
    public void updateRole(Long userId, UserRole role, String actingAdminEmail) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        if (user.getEmail().equalsIgnoreCase(actingAdminEmail) && role != UserRole.ADMIN) {
            throw new IllegalArgumentException("Role akun yang sedang digunakan tidak dapat diturunkan.");
        }
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN
                && users.countByRole(UserRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("Setidaknya satu admin harus tetap ada.");
        }
        user.setRole(role);
    }

    @Transactional
    public AppUser createUser(AdminUserForm form) {
        String email = form.getEmail().trim();
        String nim = form.getNim().trim();
        if (users.existsByEmail(email.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Email tersebut sudah terdaftar.");
        }
        if (users.existsByNimIgnoreCase(nim)) {
            throw new IllegalArgumentException("NIM tersebut sudah digunakan akun lain.");
        }
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new IllegalArgumentException("Kata sandi wajib diisi untuk akun baru.");
        }
        if (form.getPassword().length() < 8) {
            throw new IllegalArgumentException("Kata sandi harus memiliki minimal 8 karakter.");
        }
        AppUser user = new AppUser(form.getFullName().trim(), email,
                passwordEncoder.encode(form.getPassword()));
        user.updateProfile(form.getFullName(), nim, form.getStudyProgram());
        user.setRole(form.getRole());
        return users.save(user);
    }

    @Transactional
    public void updateUser(Long userId, AdminUserForm form, String actingAdminEmail) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        if (user.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new IllegalArgumentException("Gunakan halaman profil untuk mengubah data admin yang sedang digunakan.");
        }
        String email = form.getEmail().trim();
        String nim = form.getNim().trim();
        if (users.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new IllegalArgumentException("Email tersebut sudah digunakan akun lain.");
        }
        if (users.existsByNimIgnoreCaseAndIdNot(nim, userId)) {
            throw new IllegalArgumentException("NIM tersebut sudah digunakan akun lain.");
        }
        ensureAdminRemains(user, form.getRole());
        user.updateAccount(form.getFullName(), nim, form.getStudyProgram(), email, form.getRole());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 8) {
                throw new IllegalArgumentException("Kata sandi harus memiliki minimal 8 karakter.");
            }
            user.updatePassword(passwordEncoder.encode(form.getPassword()));
        }
    }

    @Transactional
    public void deleteUser(Long userId, String actingAdminEmail) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
        if (user.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new IllegalArgumentException("Akun admin yang sedang digunakan tidak dapat dihapus.");
        }
        ensureAdminRemains(user, null);
        for (LostFoundReport report : reports.findByOwnerIdOrderByCreatedAtDesc(userId)) {
            removeReport(report);
        }
        for (ItemClaim claim : claims.findByClaimantIdOrderByCreatedAtDesc(userId)) {
            if (claim.getStatus() == ClaimStatus.ACCEPTED
                    && claim.getReport().getStatus() == ReportStatus.CLAIMED) {
                claim.getReport().setStatus(ReportStatus.OPEN);
            }
        }
        for (ReportComment comment : comments.findByAuthorId(userId)) {
            uploadStorage.delete(comment.getImagePath());
        }
        claims.deleteByClaimantId(userId);
        notifications.deleteByRecipientId(userId);
        comments.deleteByAuthorId(userId);
        users.delete(user);
    }

    @Transactional
    public void deleteReport(Long reportId) {
        LostFoundReport report = reports.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Laporan tidak ditemukan."));
        removeReport(report);
    }

    private void ensureAdminRemains(AppUser user, UserRole newRole) {
        if (user.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN
                && users.countByRole(UserRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("Setidaknya satu admin harus tetap ada.");
        }
    }

    private void removeReport(LostFoundReport report) {
        uploadStorage.delete(report.getImagePath());
        for (ReportComment comment : comments.findByReportId(report.getId())) {
            uploadStorage.delete(comment.getImagePath());
        }
        notifications.deleteByReportId(report.getId());
        comments.deleteByReportId(report.getId());
        claims.deleteByReportId(report.getId());
        reports.delete(report);
    }
}
