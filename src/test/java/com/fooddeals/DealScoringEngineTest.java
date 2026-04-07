package com.fooddeals;

import com.fooddeals.entity.Deal;
import com.fooddeals.util.DealScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DealScoringEngineTest {

    private DealScoringEngine scoringEngine;

    @BeforeEach
    void setUp() {
        scoringEngine = new DealScoringEngine();
    }

    private Deal buildDeal(Double original, Double discounted, Double discountPct, int minutesOld) {
        return Deal.builder()
                .restaurantName("Test Restaurant")
                .originalPrice(original)
                .discountedPrice(discounted)
                .discountPercent(discountPct)
                .scrapedAt(LocalDateTime.now().minusMinutes(minutesOld))
                .build();
    }

    @Test
    @DisplayName("Higher rupee savings should score higher than lower savings")
    void higherSavingsShouldScoreHigher() {
        // ₹200 saved (40% off ₹500)
        Deal bigSavings = buildDeal(500.0, 300.0, 40.0, 5);
        // ₹50 saved (50% off ₹100)
        Deal smallSavings = buildDeal(100.0, 50.0, 50.0, 5);

        double bigScore   = scoringEngine.computeScore(bigSavings);
        double smallScore = scoringEngine.computeScore(smallSavings);

        assertTrue(bigScore > smallScore,
            "₹200 savings should score higher than ₹50 savings, even if % is lower");
    }

    @Test
    @DisplayName("Fresh deals should score higher than stale ones")
    void freshDealsShouldScoreHigher() {
        Deal freshDeal = buildDeal(300.0, 150.0, 50.0, 10);    // 10 min old
        Deal staleDeal = buildDeal(300.0, 150.0, 50.0, 23 * 60); // 23 hours old

        double freshScore = scoringEngine.computeScore(freshDeal);
        double staleScore = scoringEngine.computeScore(staleDeal);

        assertTrue(freshScore > staleScore,
            "A deal scraped 10 minutes ago should score higher than one from 23 hours ago");
    }

    @Test
    @DisplayName("Score should always be between 0 and 100")
    void scoreShouldBeWithinBounds() {
        Deal deal1 = buildDeal(1000.0, 100.0, 90.0, 0);
        Deal deal2 = buildDeal(50.0, 45.0, 10.0, 23 * 60);
        Deal deal3 = buildDeal(null, null, null, 5);

        assertAll(
            () -> assertTrue(scoringEngine.computeScore(deal1) <= 100.0),
            () -> assertTrue(scoringEngine.computeScore(deal1) >= 0.0),
            () -> assertTrue(scoringEngine.computeScore(deal2) >= 0.0),
            () -> assertTrue(scoringEngine.computeScore(deal3) >= 0.0)
        );
    }

    @Test
    @DisplayName("Deal with no price info should still get a non-zero score")
    void noPriceInfoShouldStillScore() {
        Deal deal = buildDeal(null, null, 30.0, 30);
        double score = scoringEngine.computeScore(deal);
        assertTrue(score > 0, "Even without price data, a 30% deal should score > 0");
    }

    @Test
    @DisplayName("Cheaper final price should score higher than expensive one at same discount %")
    void affordabilityMatters() {
        Deal budgetMeal  = buildDeal(200.0, 120.0, 40.0, 5);  // ₹120 final
        Deal expensiveMeal = buildDeal(1000.0, 600.0, 40.0, 5); // ₹600 final

        double budgetScore   = scoringEngine.computeScore(budgetMeal);
        double expensiveScore = scoringEngine.computeScore(expensiveMeal);

        assertTrue(budgetScore > expensiveScore,
            "A ₹120 meal should score higher than a ₹600 meal at the same discount %");
    }
}
