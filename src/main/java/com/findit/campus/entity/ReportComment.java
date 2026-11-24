package com.findit.campus.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_comments")
public class ReportComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LostFoundReport report;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(length = 1200)
    private String message;

    @Column(length = 300)
    private String imagePath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ReportComment() {
    }

    public ReportComment(LostFoundReport report, AppUser author, String message, String imagePath) {
        this.report = report;
        this.author = author;
        this.message = message;
        this.imagePath = imagePath;
    }

    public Long getId() { return id; }
    public LostFoundReport getReport() { return report; }
    public AppUser getAuthor() { return author; }
    public String getMessage() { return message; }
    public String getImagePath() { return imagePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void update(String message, String imagePath) {
        this.message = message;
        this.imagePath = imagePath;
    }
}
