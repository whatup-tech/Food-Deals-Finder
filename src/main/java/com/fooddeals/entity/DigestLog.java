package com.fooddeals.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "digest_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DigestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    @Column(nullable = false)
    private Integer dealsIncluded;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    private Boolean success;
    private String errorMessage;
}
