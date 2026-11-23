package com.findit.campus.repository;

import com.findit.campus.entity.ItemClaim;
import com.findit.campus.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<ItemClaim, Long> {
    List<ItemClaim> findByClaimantIdOrderByCreatedAtDesc(Long claimantId);
    List<ItemClaim> findByReportOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<ItemClaim> findByReportIdOrderByCreatedAtDesc(Long reportId);
    List<ItemClaim> findByReportIdAndStatus(Long reportId, ClaimStatus status);
    long countByStatus(ClaimStatus status);
    void deleteByReportId(Long reportId);
    boolean existsByReportIdAndClaimantId(Long reportId, Long claimantId);
}
