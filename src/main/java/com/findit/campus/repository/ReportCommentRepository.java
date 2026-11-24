package com.findit.campus.repository;

import com.findit.campus.entity.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    List<ReportComment> findByReportIdOrderByCreatedAtAsc(Long reportId);
    List<ReportComment> findByAuthorId(Long authorId);
    void deleteByAuthorId(Long authorId);
    List<ReportComment> findByReportId(Long reportId);
    void deleteByReportId(Long reportId);
}
