package com.fooddeals.repository;

import com.fooddeals.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmailIgnoreCase(String email);
    List<Subscriber> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Subscriber> findByIsActiveTrue();
    boolean existsByEmailIgnoreCase(String email);
}
