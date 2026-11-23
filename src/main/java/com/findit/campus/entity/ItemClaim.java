package com.findit.campus.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_claims", uniqueConstraints = @UniqueConstraint(columnNames = {"report_id", "claimant_id"}))
public class ItemClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LostFoundReport report;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private AppUser claimant;

    @Column(nullable = false, length = 1200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ItemClaim() {
    }

    public ItemClaim(LostFoundReport report, AppUser claimant, String message) {
        this.report = report;
        this.claimant = claimant;
        this.message = message;
    }

    public Long getId() { return id; }
    public LostFoundReport getReport() { return report; }
    public AppUser getClaimant() { return claimant; }
    public String getMessage() { return message; }
    public ClaimStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }
}
