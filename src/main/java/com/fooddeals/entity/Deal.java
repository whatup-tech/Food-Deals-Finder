package com.fooddeals.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String restaurantName;

    @Column(nullable = false)
    private String cuisine;          // "Indian", "Chinese", "Pizza" etc.

    @Column(nullable = false, length = 500)
    private String title;            // "50% off on orders above ₹199"

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String source;           // "Swiggy", "Zomato", "Direct"

    @Column(nullable = false)
    private String sourceUrl;

    private Double originalPrice;
    private Double discountedPrice;
    private Double discountPercent;

    @Column(nullable = false)
    private Double valueScore;       // Our computed score — the key feature

    private String city;
    private String area;

    private LocalDateTime validUntil;

    @Column(nullable = false)
    private LocalDateTime scrapedAt;

    @Column(nullable = false)
    private Boolean isActive = true;
}
