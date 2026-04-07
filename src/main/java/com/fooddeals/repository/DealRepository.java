package com.fooddeals.repository;

import com.fooddeals.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    // Top deals by city, sorted by our value score
    List<Deal> findByCityIgnoreCaseAndIsActiveTrueOrderByValueScoreDesc(String city);

    // Top deals filtered by cuisine + city
    List<Deal> findByCityIgnoreCaseAndCuisineIgnoreCaseAndIsActiveTrueOrderByValueScoreDesc(
            String city, String cuisine);

    // Deals by area within a city
    List<Deal> findByCityIgnoreCaseAndAreaIgnoreCaseAndIsActiveTrueOrderByValueScoreDesc(
            String city, String area);

    // Top N deals for a city (used in digest)
    @Query("""
        SELECT d FROM Deal d
        WHERE LOWER(d.city) = LOWER(:city)
          AND d.isActive = true
          AND d.scrapedAt >= :since
        ORDER BY d.valueScore DESC
        LIMIT :limit
    """)
    List<Deal> findTopDealsForCity(
            @Param("city") String city,
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);

    // Personalised: match subscriber's cuisines
    @Query("""
        SELECT d FROM Deal d
        WHERE LOWER(d.city) = LOWER(:city)
          AND d.isActive = true
          AND d.scrapedAt >= :since
          AND LOWER(d.cuisine) IN :cuisines
        ORDER BY d.valueScore DESC
        LIMIT :limit
    """)
    List<Deal> findTopDealsForCityAndCuisines(
            @Param("city") String city,
            @Param("since") LocalDateTime since,
            @Param("cuisines") List<String> cuisines,
            @Param("limit") int limit);

    // Cleanup: mark stale deals inactive
    @Query("UPDATE Deal d SET d.isActive = false WHERE d.scrapedAt < :cutoff")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deactivateStaleDeals(@Param("cutoff") LocalDateTime cutoff);

    // Source breakdown stats
    @Query("SELECT d.source, COUNT(d) FROM Deal d WHERE d.isActive = true GROUP BY d.source")
    List<Object[]> countBySource();
}
