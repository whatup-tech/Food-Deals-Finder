package com.fooddeals.service;

import com.fooddeals.entity.Subscriber;
import com.fooddeals.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    @Transactional
    public Subscriber subscribe(String email, String city, String area, String cuisines, Integer maxDistanceKm) {
        if (subscriberRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already subscribed: " + email);
        }
        Subscriber sub = Subscriber.builder()
                .email(email)
                .city(city)
                .area(area)
                .preferredCuisines(cuisines)
                .maxDistanceKm(maxDistanceKm)
                .isActive(true)
                .build();
        return subscriberRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(String email) {
        subscriberRepository.findByEmailIgnoreCase(email).ifPresent(sub -> {
            sub.setIsActive(false);
            subscriberRepository.save(sub);
            log.info("Unsubscribed: {}", email);
        });
    }

    public List<Subscriber> getAllActiveSubscribers() {
        return subscriberRepository.findByIsActiveTrue();
    }

    public List<Subscriber> getSubscribersByCity(String city) {
        return subscriberRepository.findByCityIgnoreCaseAndIsActiveTrue(city);
    }
}
