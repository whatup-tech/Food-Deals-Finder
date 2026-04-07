package com.fooddeals.controller;

import com.fooddeals.dto.Dtos;
import com.fooddeals.entity.Subscriber;
import com.fooddeals.service.DigestEmailService;
import com.fooddeals.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final DigestEmailService digestEmailService;

    /**
     * POST /api/subscribers/subscribe
     * Subscribe to the daily digest.
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Dtos.ApiResponse<String>> subscribe(
            @Valid @RequestBody Dtos.SubscribeRequest request) {
        try {
            Subscriber sub = subscriberService.subscribe(
                    request.getEmail(),
                    request.getCity(),
                    request.getArea(),
                    request.getPreferredCuisines(),
                    request.getMaxDistanceKm()
            );
            return ResponseEntity.ok(Dtos.ApiResponse.ok(sub.getEmail(),
                    "Subscribed! You'll get daily deals for " + sub.getCity()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Dtos.ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/subscribers/unsubscribe?email=xyz@gmail.com
     * Unsubscribe — GET so it works as a link in the email.
     */
    @GetMapping("/unsubscribe")
    public ResponseEntity<Dtos.ApiResponse<String>> unsubscribe(@RequestParam String email) {
        subscriberService.unsubscribe(email);
        return ResponseEntity.ok(Dtos.ApiResponse.ok(email, "Unsubscribed successfully."));
    }

    /**
     * POST /api/subscribers/test-digest?email=xyz@gmail.com
     * Manually trigger a digest for testing — very useful for demo.
     */
    @PostMapping("/test-digest")
    public ResponseEntity<Dtos.ApiResponse<String>> testDigest(@RequestParam String email) {
        subscriberService.getAllActiveSubscribers().stream()
                .filter(s -> s.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .ifPresent(digestEmailService::sendDigest);
        return ResponseEntity.ok(Dtos.ApiResponse.ok(email, "Digest triggered for " + email));
    }
}
