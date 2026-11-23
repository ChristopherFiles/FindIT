package com.findit.campus.config;

import com.findit.campus.entity.CampusLocation;
import com.findit.campus.repository.LocationRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LocationInitializer {
    @Bean
    ApplicationRunner seedCampusLocations(LocationRepository locations) {
        return args -> List.of("Perpustakaan", "Gedung Kuliah A", "Gedung Kuliah B",
                        "Laboratorium", "Kantin", "Auditorium", "Area Parkir", "Lapangan", "Lainnya")
                .forEach(name -> {
                    if (!locations.existsByName(name)) {
                        locations.save(new CampusLocation(name));
                    }
                });
    }
}
