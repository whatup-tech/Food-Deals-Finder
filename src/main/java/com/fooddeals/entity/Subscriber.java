package com.fooddeals.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "subscribers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String city;

    private String area;

    // Comma-separated: "Indian,Chinese,Pizza"
    private String preferredCuisines;

    // Max distance willing to travel (km)
    private Integer maxDistanceKm;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    @PrePersist
    public void prePersist() {
        this.subscribedAt = LocalDateTime.now();
    }

    public List<String> getCuisineList() {
        if (preferredCuisines == null || preferredCuisines.isBlank()) return List.of();
        return List.of(preferredCuisines.split(","));
    }
}
