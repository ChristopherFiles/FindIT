package com.findit.campus.repository;

import com.findit.campus.entity.CampusLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<CampusLocation, Long> {
    List<CampusLocation> findAllByOrderByNameAsc();
    boolean existsByName(String name);
}
