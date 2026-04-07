package com.fooddeals.repository;

import com.fooddeals.entity.DigestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigestLogRepository extends JpaRepository<DigestLog, Long> {
    List<DigestLog> findBySubscriberIdOrderBySentAtDesc(Long subscriberId);
}
