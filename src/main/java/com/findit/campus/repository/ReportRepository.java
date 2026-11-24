package com.findit.campus.repository;

import com.findit.campus.entity.LostFoundReport;
import com.findit.campus.entity.ReportStatus;
import com.findit.campus.entity.ReportType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<LostFoundReport, Long> {
    @Query("""
            select r from LostFoundReport r
            where lower(r.title) like lower(concat('%', :query, '%'))
                   or lower(r.description) like lower(concat('%', :query, '%'))
                   or lower(r.location.name) like lower(concat('%', :query, '%'))
            order by r.createdAt desc
            """)
    List<LostFoundReport> search(@Param("query") String query);

    @Query("""
            select r from LostFoundReport r
            where r.type = :type
              and (lower(r.title) like lower(concat('%', :query, '%'))
                   or lower(r.description) like lower(concat('%', :query, '%'))
                   or lower(r.location.name) like lower(concat('%', :query, '%')))
            order by r.createdAt desc
            """)
    List<LostFoundReport> searchByType(@Param("query") String query, @Param("type") ReportType type);

    List<LostFoundReport> findTop6ByOrderByCreatedAtDesc();
    List<LostFoundReport> findAllByOrderByCreatedAtDesc();
    List<LostFoundReport> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<LostFoundReport> findByTypeOrderByCreatedAtDesc(ReportType type);
    long countByType(ReportType type);
    List<LostFoundReport> findByTypeAndStatus(ReportType type, ReportStatus status);

    @Query("select r from LostFoundReport r where r.id = :id")
    Optional<LostFoundReport> findDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LostFoundReport r where r.id = :id")
    Optional<LostFoundReport> findByIdForUpdate(@Param("id") Long id);
}
