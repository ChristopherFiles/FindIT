package com.findit.campus.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_notifications")
public class AppNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private AppUser recipient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LostFoundReport report;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected AppNotification() {
    }

    public AppNotification(AppUser recipient, LostFoundReport report, String message) {
        this.recipient = recipient;
        this.report = report;
        this.message = message;
    }

    public Long getId() { return id; }
    public AppUser getRecipient() { return recipient; }
    public LostFoundReport getReport() { return report; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void markRead() { this.read = true; }
}
