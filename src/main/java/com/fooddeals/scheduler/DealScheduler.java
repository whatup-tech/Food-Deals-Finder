package com.fooddeals.scheduler;

import com.fooddeals.entity.Subscriber;
import com.fooddeals.service.DealService;
import com.fooddeals.service.DigestEmailService;
import com.fooddeals.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class DealScheduler {

    private final DealService dealService;
    private final SubscriberService subscriberService;
    private final DigestEmailService digestEmailService;

    /**
     * Refresh deals every 6 hours for all cities that have subscribers.
     * Cron: every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void refreshDealsForAllCities() {
        log.info("Starting scheduled deal refresh...");

        Set<String> cities = subscriberService.getAllActiveSubscribers()
                .stream()
                .map(Subscriber::getCity)
                .collect(Collectors.toSet());

        if (cities.isEmpty()) {
            // Default refresh for popular cities even without subscribers
            cities = Set.of("bangalore", "chennai", "mumbai");
        }

        for (String city : cities) {
            try {
                dealService.refreshDeals(city);
            } catch (Exception e) {
                log.error("Failed to refresh deals for {}: {}", city, e.getMessage());
            }
        }

        log.info("Deal refresh complete for {} cities", cities.size());
    }

    /**
     * Send daily digest at noon.
     * Configurable via app.digest.cron in application.properties.
     */
    @Scheduled(cron = "${app.digest.cron:0 0 12 * * *}")
    public void sendDailyDigests() {
        log.info("Starting daily digest send...");

        List<Subscriber> subscribers = subscriberService.getAllActiveSubscribers();
        log.info("Sending digest to {} subscribers", subscribers.size());

        int success = 0, failed = 0;
        for (Subscriber subscriber : subscribers) {
            try {
                digestEmailService.sendDigest(subscriber);
                success++;
                // Small delay to avoid hitting SMTP rate limits
                Thread.sleep(500);
            } catch (Exception e) {
                failed++;
                log.error("Digest failed for {}: {}", subscriber.getEmail(), e.getMessage());
            }
        }

        log.info("Digest complete — sent: {}, failed: {}", success, failed);
    }
}
