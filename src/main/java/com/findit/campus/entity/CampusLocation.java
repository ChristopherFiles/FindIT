package com.findit.campus.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_locations")
public class CampusLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    protected CampusLocation() {
    }

    public CampusLocation(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
