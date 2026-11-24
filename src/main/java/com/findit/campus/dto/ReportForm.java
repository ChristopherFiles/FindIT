package com.findit.campus.dto;

import com.findit.campus.entity.ItemCategory;
import com.findit.campus.entity.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class ReportForm {
    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 3000)
    private String description;

    @NotNull
    private ReportType type;

    @NotNull
    private ItemCategory category;

    @NotNull
    private Long locationId;

    @NotNull
    private LocalDateTime occurredAt;

    private MultipartFile image;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
