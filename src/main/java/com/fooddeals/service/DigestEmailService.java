package com.fooddeals.service;

import com.fooddeals.entity.Deal;
import com.fooddeals.entity.DigestLog;
import com.fooddeals.entity.Subscriber;
import com.fooddeals.repository.DigestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigestEmailService {

    private final JavaMailSender mailSender;
    private final DealService dealService;
    private final DigestLogRepository digestLogRepository;

    private static final int DEALS_PER_DIGEST = 5;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Send a personalised digest to one subscriber.
     */
    public void sendDigest(Subscriber subscriber) {
        List<Deal> deals = dealService.getTopDealsForDigest(
                subscriber.getCity(),
                subscriber.getCuisineList(),
                DEALS_PER_DIGEST
        );

        if (deals.isEmpty()) {
            log.info("No deals found for subscriber {} in {}", subscriber.getEmail(), subscriber.getCity());
            return;
        }

        DigestLog logEntry = DigestLog.builder()
                .subscriber(subscriber)
                .dealsIncluded(deals.size())
                .sentAt(LocalDateTime.now())
                .build();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(subscriber.getEmail());
            helper.setSubject("🍔 Your Daily Food Deals — " + subscriber.getCity() + " | " + LocalDateTime.now().format(FMT));
            helper.setText(buildHtmlEmail(subscriber, deals), true);

            mailSender.send(message);

            logEntry.setSuccess(true);
            log.info("Digest sent to {} with {} deals", subscriber.getEmail(), deals.size());

        } catch (Exception e) {
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Failed to send digest to {}: {}", subscriber.getEmail(), e.getMessage());
        }

        digestLogRepository.save(logEntry);
    }

    /**
     * Build a clean HTML email — looks good in Gmail.
     */
    private String buildHtmlEmail(Subscriber subscriber, List<Deal> deals) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; color: #333;">
            <div style="background: #ff6b35; padding: 24px; border-radius: 8px 8px 0 0; text-align: center;">
              <h1 style="color: white; margin: 0;">Today's Best Food Deals</h1>
              <p style="color: #ffe0d0; margin: 8px 0 0;">
            """);
        sb.append(subscriber.getCity());
        sb.append(" — Ranked by value, not just discount %</p>\n")
          .append("</div>\n")
          .append("<div style=\"background: #fff8f5; padding: 20px; border-radius: 0 0 8px 8px;\">\n");

        for (int i = 0; i < deals.size(); i++) {
            Deal d = deals.get(i);
            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> "⭐";
            };
            sb.append("""
                <div style="background: white; border-radius: 8px; padding: 16px; margin-bottom: 12px;
                            border-left: 4px solid #ff6b35; box-shadow: 0 1px 4px rgba(0,0,0,0.06);">
                  <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span style="font-size:18px;">%s %s</span>
                    <span style="background: #fff3ee; color: #c84b00; padding: 4px 10px;
                                 border-radius: 20px; font-size: 13px; font-weight: bold;">
                      Score: %.1f/100
                    </span>
                  </div>
                  <p style="margin: 8px 0 4px; color: #555; font-size: 14px;">%s</p>
                  <div style="font-size: 13px; color: #888;">
                    📍 %s &nbsp;|&nbsp; 🍽️ %s &nbsp;|&nbsp; 📱 %s
                  </div>
                  %s
                  <a href="%s" style="display:inline-block; margin-top:10px; background:#ff6b35;
                     color:white; padding:8px 16px; border-radius:6px; text-decoration:none; font-size:13px;">
                    View Deal →
                  </a>
                </div>
                """.formatted(
                    medal,
                    d.getRestaurantName(),
                    d.getValueScore(),
                    d.getTitle(),
                    d.getArea() != null ? d.getArea() : d.getCity(),
                    d.getCuisine(),
                    d.getSource(),
                    buildPriceTag(d),
                    d.getSourceUrl()
                ));
        }

        sb.append("""
            <p style="text-align:center; font-size:12px; color:#aaa; margin-top:20px;">
              You're receiving this because you subscribed to Food Deal Finder.<br>
              <a href="http://localhost:8080/api/subscribers/unsubscribe?email=""");
        sb.append(subscriber.getEmail());
        sb.append("""
            " style="color:#aaa;">Unsubscribe</a>
            </p></div></body></html>
            """);

        return sb.toString();
    }

    private String buildPriceTag(Deal deal) {
        if (deal.getOriginalPrice() == null) return "";
        return """
            <div style="margin-top: 6px; font-size: 13px;">
              <span style="text-decoration: line-through; color: #aaa;">₹%.0f</span>
              <span style="color: #ff6b35; font-weight: bold; margin-left: 8px;">₹%.0f</span>
              <span style="color: #4caf50; margin-left: 8px;">Save ₹%.0f</span>
            </div>
            """.formatted(
                deal.getOriginalPrice(),
                deal.getDiscountedPrice(),
                deal.getOriginalPrice() - deal.getDiscountedPrice()
            );
    }
}
