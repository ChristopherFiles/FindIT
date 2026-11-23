package com.findit.campus.service;

import com.findit.campus.entity.*;
import com.findit.campus.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FindItService {
    private final ReportRepository reports;
    private final LocationRepository locations;
    private final ClaimRepository claims;
    private final UserRepository users;

    public FindItService(ReportRepository reports, LocationRepository locations,
                         ClaimRepository claims, UserRepository users) {
        this.reports = reports;
        this.locations = locations;
        this.claims = claims;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CampusLocation> locations() {
        return locations.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<LostFoundReport> search(String query, ReportType type) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return reports.search(normalized, type);
    }

    @Transactional(readOnly = true)
    public List<LostFoundReport> latestReports() {
        return reports.findTop6ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countReports() { return reports.count(); }

    @Transactional(readOnly = true)
    public long countFound() { return reports.countByType(ReportType.FOUND); }

    @Transactional(readOnly = true)
    public List<LostFoundReport> myReports(Long userId) {
        return reports.findByOwnerIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public LostFoundReport createReport(String title, String description, ReportType type,
                                        ItemCategory category, Long locationId, LocalDateTime occurredAt,
                                        String email) {
        CampusLocation location = locations.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Lokasi kampus tidak ditemukan."));
        AppUser owner = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Akun tidak ditemukan."));
        return reports.save(new LostFoundReport(title.trim(), description.trim(), type, category,
                location, owner, occurredAt));
    }

    @Transactional(readOnly = true)
    public LostFoundReport report(Long id) {
        return reports.findDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Laporan tidak ditemukan."));
    }

    @Transactional
    public ItemClaim createClaim(Long reportId, String message, String email) {
        LostFoundReport report = report(reportId);
        AppUser claimant = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Akun tidak ditemukan."));
        if (report.getType() != ReportType.FOUND || report.getStatus() != ReportStatus.OPEN) {
            throw new IllegalArgumentException("Barang ini tidak tersedia untuk diklaim.");
        }
        if (report.getOwner().getId().equals(claimant.getId())) {
            throw new IllegalArgumentException("Kamu tidak dapat mengklaim laporan milikmu sendiri.");
        }
        if (claims.existsByReportIdAndClaimantId(reportId, claimant.getId())) {
            throw new IllegalArgumentException("Kamu sudah mengirim klaim untuk barang ini.");
        }
        return claims.save(new ItemClaim(report, claimant, message.trim()));
    }

    @Transactional(readOnly = true)
    public List<ItemClaim> myClaims(Long userId) {
        return claims.findByClaimantIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ItemClaim> claimsForOwner(Long userId) {
        return claims.findByReportOwnerIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<ItemClaim> claimsForReport(Long reportId) {
        return claims.findByReportIdOrderByCreatedAtDesc(reportId);
    }

    @Transactional
    public void decideClaim(Long claimId, ClaimStatus decision, String email) {
        if (decision == ClaimStatus.PENDING) {
            throw new IllegalArgumentException("Pilihan keputusan klaim tidak valid.");
        }
        ItemClaim claim = claims.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Klaim tidak ditemukan."));
        if (!claim.getReport().getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Hanya pelapor yang dapat memutuskan klaim ini.");
        }
        LostFoundReport report = reports.findByIdForUpdate(claim.getReport().getId())
                .orElseThrow(() -> new IllegalArgumentException("Laporan tidak ditemukan."));
        if (claim.getStatus() != ClaimStatus.PENDING
                || report.getStatus() != ReportStatus.OPEN) {
            throw new IllegalArgumentException("Klaim ini sudah diproses.");
        }
        claim.setStatus(decision);
        if (decision == ClaimStatus.ACCEPTED) {
            report.setStatus(ReportStatus.CLAIMED);
            claims.findByReportIdAndStatus(report.getId(), ClaimStatus.PENDING).stream()
                    .filter(other -> !other.getId().equals(claim.getId()))
                    .forEach(other -> other.setStatus(ClaimStatus.REJECTED));
        }
    }

    @Transactional
    public void markReturned(Long reportId, String email) {
        LostFoundReport report = report(reportId);
        if (!report.getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Hanya pelapor yang dapat memperbarui status barang.");
        }
        if (report.getStatus() != ReportStatus.CLAIMED) {
            throw new IllegalArgumentException("Barang harus memiliki klaim yang disetujui.");
        }
        report.setStatus(ReportStatus.RETURNED);
    }
}
