package com.fooddeals.util;

import com.fooddeals.entity.Deal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The core algorithm that makes this project interesting.
 *
 * A deal is NOT just about the highest discount percentage.
 * A 70% off on a ₹2000 meal is worse than 40% off on a ₹300 meal
 * if you only wanted a quick lunch.
 *
 * We score on 4 factors:
 *   1. Discount magnitude     — how much money you actually save (₹ saved)
 *   2. Discount ratio         — % off, normalized
 *   3. Affordability          — lower final price scores higher
 *   4. Freshness              — newer deals score higher (decays over time)
 *
 * Final score = weighted sum, normalized to 0–100.
 */
@Component
public class DealScoringEngine {

    // Weights — must sum to 1.0
    private static final double WEIGHT_SAVINGS_AMOUNT  = 0.30;
    private static final double WEIGHT_DISCOUNT_RATIO  = 0.35;
    private static final double WEIGHT_AFFORDABILITY   = 0.20;
    private static final double WEIGHT_FRESHNESS       = 0.15;

    // Normalization caps
    private static final double MAX_SAVINGS_INR  = 500.0;   // ₹500 savings = perfect score
    private static final double MAX_PRICE_INR    = 1000.0;  // ₹1000+ = least affordable
    private static final double FRESHNESS_HOURS  = 24.0;    // deals older than 24h score 0

    public double computeScore(Deal deal) {
        double savingsScore     = computeSavingsScore(deal);
        double discountScore    = computeDiscountScore(deal);
        double affordability    = computeAffordabilityScore(deal);
        double freshness        = computeFreshnessScore(deal);

        double raw = (savingsScore    * WEIGHT_SAVINGS_AMOUNT)
                   + (discountScore   * WEIGHT_DISCOUNT_RATIO)
                   + (affordability   * WEIGHT_AFFORDABILITY)
                   + (freshness       * WEIGHT_FRESHNESS);

        // Normalize to 0–100
        return Math.round(raw * 100.0 * 10.0) / 10.0;
    }

    /**
     * Actual rupee savings — more money saved = higher score.
     * Capped at MAX_SAVINGS_INR to prevent outliers dominating.
     */
    private double computeSavingsScore(Deal deal) {
        if (deal.getOriginalPrice() == null || deal.getDiscountedPrice() == null) {
            // No price info: use discount % as proxy, penalised
            return deal.getDiscountPercent() != null
                    ? (deal.getDiscountPercent() / 100.0) * 0.5
                    : 0.3; // unknown — middle-of-road
        }
        double saved = deal.getOriginalPrice() - deal.getDiscountedPrice();
        return Math.min(saved / MAX_SAVINGS_INR, 1.0);
    }

    /**
     * Discount percentage — normalized 0–1.
     * 10% = 0.1, 50% = 0.5, 100% = 1.0
     */
    private double computeDiscountScore(Deal deal) {
        if (deal.getDiscountPercent() == null) return 0.2;
        return Math.min(deal.getDiscountPercent() / 100.0, 1.0);
    }

    /**
     * Affordability — cheaper final price scores higher.
     * Score = 1 - (finalPrice / MAX_PRICE_INR), clamped to [0,1].
     * A ₹100 meal scores 0.9; a ₹1000 meal scores 0.0.
     */
    private double computeAffordabilityScore(Deal deal) {
        Double finalPrice = deal.getDiscountedPrice() != null
                ? deal.getDiscountedPrice()
                : deal.getOriginalPrice();

        if (finalPrice == null) return 0.5; // unknown price — neutral
        double score = 1.0 - (finalPrice / MAX_PRICE_INR);
        return Math.max(0.0, Math.min(score, 1.0));
    }

    /**
     * Freshness — deals decay linearly over FRESHNESS_HOURS.
     * A deal scraped just now = 1.0; 12 hours ago = 0.5; 24h+ = 0.0.
     */
    private double computeFreshnessScore(Deal deal) {
        if (deal.getScrapedAt() == null) return 0.0;
        long hoursOld = ChronoUnit.HOURS.between(deal.getScrapedAt(), LocalDateTime.now());
        double score = 1.0 - (hoursOld / FRESHNESS_HOURS);
        return Math.max(0.0, Math.min(score, 1.0));
    }
}
