package com.fooddeals.service;

import com.fooddeals.entity.Deal;
import com.fooddeals.repository.DealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final DealScraperService scraperService;

    @Value("${app.deals.max-age-hours:24}")
    private int maxAgeHours;

    /**
     * Refresh deals for a city — scrape and persist.
     */
    @Transactional
    public List<Deal> refreshDeals(String city) {
        log.info("Refreshing deals for city: {}", city);

        // Deactivate stale deals first
        dealRepository.deactivateStaleDeals(LocalDateTime.now().minusHours(maxAgeHours));

        // Scrape fresh deals
        List<Deal> freshDeals = scraperService.scrapeAllSources(city);

        // Save all
        List<Deal> saved = dealRepository.saveAll(freshDeals);
        log.info("Saved {} deals for {}", saved.size(), city);
        return saved;
    }

    /**
     * Get top deals for a city, sorted by value score.
     */
    public List<Deal> getTopDeals(String city, String cuisine, String area) {
        if (cuisine != null && !cuisine.isBlank()) {
            return dealRepository
                    .findByCityIgnoreCaseAndCuisineIgnoreCaseAndIsActiveTrueOrderByValueScoreDesc(city, cuisine);
        }
        if (area != null && !area.isBlank()) {
            return dealRepository
                    .findByCityIgnoreCaseAndAreaIgnoreCaseAndIsActiveTrueOrderByValueScoreDesc(city, area);
        }
        return dealRepository.findByCityIgnoreCaseAndIsActiveTrueOrderByValueScoreDesc(city);
    }

    /**
     * Get top N deals for a city — used by the digest scheduler.
     */
    public List<Deal> getTopDealsForDigest(String city, List<String> cuisines, int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(maxAgeHours);
        if (cuisines != null && !cuisines.isEmpty()) {
            List<String> lower = cuisines.stream().map(String::toLowerCase).toList();
            return dealRepository.findTopDealsForCityAndCuisines(city, since, lower, limit);
        }
        return dealRepository.findTopDealsForCity(city, since, limit);
    }

    /**
     * Stats endpoint — deals count by source.
     */
    public Map<String, Long> getStatsBySource() {
        return dealRepository.countBySource().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Deal getDealById(Long id) {
        return dealRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deal not found: " + id));
    }
}
