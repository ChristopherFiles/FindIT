package com.findit.campus.service;

import com.findit.campus.entity.AppUser;
import com.findit.campus.entity.ClaimStatus;
import com.findit.campus.entity.LostFoundReport;
import com.findit.campus.entity.UserRole;
import com.findit.campus.repository.ClaimRepository;
import com.findit.campus.repository.ReportRepository;
import com.findit.campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository users;
    private final ReportRepository reports;
    private final ClaimRepository claims;

    public AdminService(UserRepository users, ReportRepository reports, ClaimRepository claims) {
        this.users = users;
        this.reports = reports;
        this.claims = claims;
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
    public void deleteReport(Long reportId) {
        if (!reports.existsById(reportId)) {
            throw new IllegalArgumentException("Laporan tidak ditemukan.");
        }
        claims.deleteByReportId(reportId);
        reports.deleteById(reportId);
    }
}
