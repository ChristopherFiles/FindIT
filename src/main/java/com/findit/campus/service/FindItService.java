package com.findit.campus.service;

import com.findit.campus.dto.ReportForm;
import com.findit.campus.entity.*;
import com.findit.campus.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FindItService {
    private final ReportRepository reports;
    private final LocationRepository locations;
    private final ClaimRepository claims;
    private final UserRepository users;
    private final ReportCommentRepository comments;
    private final NotificationRepository notifications;
    private final UploadStorage uploadStorage;

    public FindItService(ReportRepository reports, LocationRepository locations,
                         ClaimRepository claims, UserRepository users, ReportCommentRepository comments,
                         NotificationRepository notifications, UploadStorage uploadStorage) {
        this.reports = reports;
        this.locations = locations;
        this.claims = claims;
        this.users = users;
        this.comments = comments;
        this.notifications = notifications;
        this.uploadStorage = uploadStorage;
    }

    @Transactional(readOnly = true)
    public List<CampusLocation> locations() {
        return locations.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<LostFoundReport> search(String query, ReportType type) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        if (normalized == null) {
            return type == null ? reports.findAllByOrderByCreatedAtDesc()
                    : reports.findByTypeOrderByCreatedAtDesc(type);
        }
        return type == null ? reports.search(normalized) : reports.searchByType(normalized, type);
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
                                        String email, MultipartFile image) {
        CampusLocation location = locations.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Lokasi kampus tidak ditemukan."));
        AppUser owner = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Akun tidak ditemukan."));
        String imagePath = uploadStorage.storeImage(image);
        LostFoundReport created = reports.save(new LostFoundReport(title.trim(), description.trim(), imagePath,
                type, category, location, owner, occurredAt));
        if (type == ReportType.FOUND) {
            notifyPossibleMatches(created);
        }
        return created;
    }

    @Transactional(readOnly = true)
    public LostFoundReport report(Long id) {
        return reports.findDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Laporan tidak ditemukan."));
    }

    @Transactional
    public void updateReport(Long id, ReportForm form, AppUser actor) {
        LostFoundReport report = report(id);
        ensureReportManager(report, actor);
        CampusLocation location = locations.findById(form.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Lokasi kampus tidak ditemukan."));
        String newImagePath = uploadStorage.storeImage(form.getImage());
        report.update(form.getTitle(), form.getDescription(), form.getType(), form.getCategory(),
                location, form.getOccurredAt());
        if (newImagePath != null) {
            uploadStorage.delete(report.getImagePath());
            report.setImagePath(newImagePath);
        }
    }

    @Transactional(readOnly = true)
    public void ensureReportManager(Long id, AppUser actor) {
        ensureReportManager(report(id), actor);
    }

    @Transactional
    public void deleteReport(Long id, AppUser actor) {
        LostFoundReport report = report(id);
        ensureReportManager(report, actor);
        uploadStorage.delete(report.getImagePath());
        comments.findByReportId(id).forEach(comment -> uploadStorage.delete(comment.getImagePath()));
        notifications.deleteByReportId(id);
        comments.deleteByReportId(id);
        claims.deleteByReportId(id);
        reports.delete(report);
    }

    @Transactional(readOnly = true)
    public List<ReportComment> commentsForReport(Long reportId) {
        return comments.findByReportIdOrderByCreatedAtAsc(reportId);
    }

    @Transactional(readOnly = true)
    public Long commentReportId(Long commentId) {
        return comments.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Komentar tidak ditemukan."))
                .getReport().getId();
    }

    @Transactional
    public ReportComment addComment(Long reportId, String message, MultipartFile image, AppUser author) {
        LostFoundReport report = report(reportId);
        String normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.isEmpty() && (image == null || image.isEmpty())) {
            throw new IllegalArgumentException("Isi komentar atau lampirkan gambar.");
        }
        String imagePath = uploadStorage.storeImage(image);
        return comments.save(new ReportComment(report, author,
                normalizedMessage.isEmpty() ? null : normalizedMessage, imagePath));
    }

    @Transactional
    public Long updateComment(Long commentId, String message, MultipartFile image, AppUser actor) {
        ReportComment comment = comments.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Komentar tidak ditemukan."));
        ensureCommentManager(comment, actor);
        String normalizedMessage = message == null ? "" : message.trim();
        boolean hasNewImage = image != null && !image.isEmpty();
        if (normalizedMessage.isEmpty() && !hasNewImage && comment.getImagePath() == null) {
            throw new IllegalArgumentException("Isi komentar atau lampirkan gambar.");
        }
        String imagePath = hasNewImage ? uploadStorage.storeImage(image) : comment.getImagePath();
        if (hasNewImage) {
            uploadStorage.delete(comment.getImagePath());
        }
        comment.update(normalizedMessage.isEmpty() ? null : normalizedMessage, imagePath);
        return comment.getReport().getId();
    }

    @Transactional
    public Long deleteComment(Long commentId, AppUser actor) {
        ReportComment comment = comments.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Komentar tidak ditemukan."));
        ensureCommentManager(comment, actor);
        uploadStorage.delete(comment.getImagePath());
        comments.delete(comment);
        return comment.getReport().getId();
    }

    @Transactional(readOnly = true)
    public List<AppNotification> notificationsForUser(Long userId) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadNotificationCount(Long userId) {
        return notifications.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public Long markNotificationRead(Long notificationId, Long userId) {
        AppNotification notification = notifications.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notifikasi tidak ditemukan."));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new AccessDeniedException("Notifikasi ini bukan milik akunmu.");
        }
        notification.markRead();
        return notification.getReport().getId();
    }

    private void notifyPossibleMatches(LostFoundReport foundReport) {
        Set<Long> notifiedOwners = reports.findByTypeAndStatus(ReportType.LOST, ReportStatus.OPEN).stream()
                .filter(lostReport -> !lostReport.getOwner().getId().equals(foundReport.getOwner().getId()))
                .filter(lostReport -> lostReport.getCategory() == foundReport.getCategory())
                .filter(lostReport -> titlesMatch(lostReport.getTitle(), foundReport.getTitle()))
                .map(lostReport -> lostReport.getOwner().getId())
                .collect(Collectors.toSet());
        for (Long ownerId : notifiedOwners) {
            AppUser recipient = users.findById(ownerId)
                    .orElseThrow(() -> new IllegalStateException("Pemilik laporan tidak ditemukan."));
            notifications.save(new AppNotification(recipient, foundReport,
                    "Ada laporan temuan yang mungkin cocok dengan barang hilangmu: " + foundReport.getTitle()));
        }
    }

    private boolean titlesMatch(String lostTitle, String foundTitle) {
        Set<String> lostWords = searchableWords(lostTitle);
        Set<String> foundWords = searchableWords(foundTitle);
        return lostWords.stream().anyMatch(foundWords::contains);
    }

    private Set<String> searchableWords(String title) {
        return Arrays.stream(title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").split("\\s+"))
                .filter(word -> word.length() >= 4)
                .filter(word -> !Set.of("barang", "hilang", "temuan", "ditemukan", "saya").contains(word))
                .collect(Collectors.toSet());
    }

    private void ensureReportManager(LostFoundReport report, AppUser actor) {
        if (actor.getRole() != UserRole.ADMIN && !report.getOwner().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Hanya pemilik laporan atau admin yang dapat mengubah laporan ini.");
        }
    }

    private void ensureCommentManager(ReportComment comment, AppUser actor) {
        if (actor.getRole() != UserRole.ADMIN && !comment.getAuthor().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Hanya pembuat komentar atau admin yang dapat mengubahnya.");
        }
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
