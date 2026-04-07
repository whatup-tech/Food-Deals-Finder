package com.fooddeals.controller;

import com.fooddeals.dto.Dtos;
import com.fooddeals.entity.Deal;
import com.fooddeals.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    /**
     * GET /api/deals?city=bangalore&cuisine=Indian&area=Koramangala
     * Returns top deals sorted by value score.
     */
    @GetMapping
    public ResponseEntity<Dtos.ApiResponse<List<Dtos.DealResponse>>> getDeals(
            @RequestParam String city,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String area) {

        List<Dtos.DealResponse> deals = dealService
                .getTopDeals(city, cuisine, area)
                .stream()
                .map(Dtos.DealResponse::from)
                .toList();

        return ResponseEntity.ok(Dtos.ApiResponse.ok(deals,
                "Found " + deals.size() + " deals in " + city));
    }

    /**
     * GET /api/deals/{id}
     * Single deal detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Dtos.ApiResponse<Dtos.DealResponse>> getDeal(@PathVariable Long id) {
        Deal deal = dealService.getDealById(id);
        return ResponseEntity.ok(Dtos.ApiResponse.ok(Dtos.DealResponse.from(deal), "Deal found"));
    }

    /**
     * POST /api/deals/refresh?city=bangalore
     * Manually trigger a scrape + refresh for a city.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Dtos.ApiResponse<Integer>> refreshDeals(@RequestParam String city) {
        List<Deal> deals = dealService.refreshDeals(city);
        return ResponseEntity.ok(Dtos.ApiResponse.ok(deals.size(),
                "Refreshed " + deals.size() + " deals for " + city));
    }

    /**
     * GET /api/deals/stats
     * Deal counts by source — shows the scraper is working.
     */
    @GetMapping("/stats")
    public ResponseEntity<Dtos.ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(Dtos.ApiResponse.ok(dealService.getStatsBySource(), "Stats fetched"));
    }
}
