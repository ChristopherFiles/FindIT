package com.findit.campus.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "item_reports")
public class LostFoundReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private CampusLocation location;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected LostFoundReport() {
    }

    public LostFoundReport(String title, String description, ReportType type, ItemCategory category,
                           CampusLocation location, AppUser owner, LocalDateTime occurredAt) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.category = category;
        this.location = location;
        this.owner = owner;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ReportType getType() { return type; }
    public ItemCategory getCategory() { return category; }
    public ReportStatus getStatus() { return status; }
    public CampusLocation getLocation() { return location; }
    public AppUser getOwner() { return owner; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }
}
