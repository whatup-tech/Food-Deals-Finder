package com.fooddeals.service;

import com.fooddeals.entity.Deal;
import com.fooddeals.util.DealScoringEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes food deals from publicly available sources.
 *
 * Note: In production, you'd add more scrapers (Zomato offers page,
 * restaurant websites, coupon aggregators). We also include a
 * SimulatedScraper so the app works out-of-the-box without network issues.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DealScraperService {

    private final DealScoringEngine scoringEngine;

    @Value("${app.scraper.timeout-ms:5000}")
    private int timeoutMs;

    /**
     * Main entry point — scrapes all sources and returns raw deals.
     * Scoring is applied here before returning.
     */
    public List<Deal> scrapeAllSources(String city) {
        List<Deal> deals = new ArrayList<>();

        // Real scraper: publicly available offer pages
        deals.addAll(scrapeZomatoOffers(city));

        // Simulated deals — realistic data so the app runs standalone
        deals.addAll(generateSimulatedDeals(city));

        // Score every deal
        deals.forEach(deal -> deal.setValueScore(scoringEngine.computeScore(deal)));

        log.info("Scraped {} deals for city: {}", deals.size(), city);
        return deals;
    }

    /**
     * Scrapes Zomato's public offers page.
     * This hits the HTML page — no private API used.
     */
    private List<Deal> scrapeZomatoOffers(String city) {
        List<Deal> deals = new ArrayList<>();
        try {
            String url = "https://www.zomato.com/" + city.toLowerCase() + "/offers";
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; FoodDealBot/1.0)")
                    .timeout(timeoutMs)
                    .get();

            // Zomato renders offer cards — parse the text content
            Elements offerCards = doc.select("[class*='offer'], [class*='deal'], [class*='discount']");

            for (Element card : offerCards) {
                String text = card.text();
                if (text.contains("%") || text.contains("off") || text.contains("₹")) {
                    Deal deal = Deal.builder()
                            .restaurantName(extractRestaurantName(card))
                            .title(text.length() > 200 ? text.substring(0, 200) : text)
                            .source("Zomato")
                            .sourceUrl(url)
                            .city(city)
                            .cuisine(detectCuisine(text))
                            .discountPercent(extractDiscountPercent(text))
                            .scrapedAt(LocalDateTime.now())
                            .isActive(true)
                            .build();

                    if (deal.getDiscountPercent() != null) {
                        deals.add(deal);
                    }
                }
            }
            log.info("Scraped {} deals from Zomato for {}", deals.size(), city);
        } catch (Exception e) {
            log.warn("Zomato scrape failed for {}: {} — using simulated data", city, e.getMessage());
        }
        return deals;
    }

    /**
     * Generates realistic simulated deals.
     * Makes the app demo-able without network access.
     */
    private List<Deal> generateSimulatedDeals(String city) {
        Random rand = new Random();
        List<Deal> deals = new ArrayList<>();

        String[][] restaurantData = {
            {"Pizza Hut", "Pizza", "50% off on medium pizzas above ₹299", "50", "299", "149"},
            {"Domino's", "Pizza", "Buy 1 Get 1 Free on all medium pizzas", "50", "259", "129"},
            {"McDonald's", "Fast Food", "20% off on McValue Meals", "20", "199", "159"},
            {"KFC", "Fast Food", "Buddy Bucket at ₹499 (worth ₹799)", "38", "799", "499"},
            {"Subway", "Healthy", "Sub of the Day at ₹179", "40", "299", "179"},
            {"Biryani Blues", "Indian", "40% off on Hyderabadi Biryani", "40", "320", "192"},
            {"Behrouz Biryani", "Indian", "Flat ₹100 off on orders above ₹500", "20", "500", "400"},
            {"Fasoos", "Indian", "Wraps starting at ₹129, combo at ₹199", "35", "299", "199"},
            {"Box8", "Indian", "3 meals for ₹499 — save ₹200", "29", "699", "499"},
            {"Chaayos", "Beverages", "Buy 2 Chais, get 1 snack free", "33", "180", "120"},
            {"The Bowl Company", "Healthy", "Protein Bowl at ₹249 (was ₹349)", "29", "349", "249"},
            {"Faasos", "Indian", "Evening Snack combo at ₹99", "50", "199", "99"},
            {"WOW! Momo", "Chinese", "Momos + Soup at ₹149", "40", "249", "149"},
            {"Haldiram's", "Snacks", "Thali at ₹199 all day", "33", "299", "199"},
            {"Barbeque Nation", "Grill", "Lunch buffet at ₹699 (was ₹999)", "30", "999", "699"},
        };

        for (String[] data : restaurantData) {
            // Vary scrape time slightly to make freshness scores differ
            int minutesAgo = rand.nextInt(120);

            Deal deal = Deal.builder()
                    .restaurantName(data[0])
                    .cuisine(data[1])
                    .title(data[2])
                    .source("Simulated")
                    .sourceUrl("https://example.com/" + data[0].toLowerCase().replace(" ", "-"))
                    .city(city)
                    .area(randomArea(city, rand))
                    .discountPercent(Double.parseDouble(data[3]))
                    .originalPrice(Double.parseDouble(data[4]))
                    .discountedPrice(Double.parseDouble(data[5]))
                    .scrapedAt(LocalDateTime.now().minusMinutes(minutesAgo))
                    .isActive(true)
                    .build();

            deals.add(deal);
        }

        return deals;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Double extractDiscountPercent(String text) {
        Pattern p = Pattern.compile("(\\d+)%\\s*off", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return Double.parseDouble(m.group(1));
        return null;
    }

    private String extractRestaurantName(Element card) {
        Element nameEl = card.selectFirst("[class*='name'], h3, h4, strong");
        return nameEl != null ? nameEl.text() : "Unknown Restaurant";
    }

    private String detectCuisine(String text) {
        text = text.toLowerCase();
        if (text.contains("pizza"))           return "Pizza";
        if (text.contains("burger"))          return "Fast Food";
        if (text.contains("biryani"))         return "Indian";
        if (text.contains("momo") || text.contains("noodle") || text.contains("chinese")) return "Chinese";
        if (text.contains("salad") || text.contains("bowl") || text.contains("wrap"))     return "Healthy";
        if (text.contains("chai") || text.contains("coffee"))                             return "Beverages";
        return "Other";
    }

    private String randomArea(String city, Random rand) {
        String[] bangaloreAreas = {"Koramangala", "Indiranagar", "HSR Layout", "BTM Layout", "Whitefield"};
        String[] chennaiareas   = {"Anna Nagar", "T Nagar", "Adyar", "Velachery", "OMR"};
        String[] mumbaiAreas    = {"Bandra", "Andheri", "Powai", "Lower Parel", "Juhu"};
        String[] defaultAreas   = {"Area 1", "Area 2", "Area 3"};

        return switch (city.toLowerCase()) {
            case "bangalore" -> bangaloreAreas[rand.nextInt(bangaloreAreas.length)];
            case "chennai"   -> chennaiareas[rand.nextInt(chennaiareas.length)];
            case "mumbai"    -> mumbaiAreas[rand.nextInt(mumbaiAreas.length)];
            default          -> defaultAreas[rand.nextInt(defaultAreas.length)];
        };
    }
}
