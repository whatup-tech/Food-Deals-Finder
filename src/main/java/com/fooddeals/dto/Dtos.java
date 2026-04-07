package com.fooddeals.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class Dtos {

    @Data
    public static class SubscribeRequest {
        @Email(message = "Invalid email address")
        @NotBlank
        private String email;

        @NotBlank(message = "City is required")
        private String city;

        private String area;
        private String preferredCuisines; // "Indian,Pizza,Chinese"
        private Integer maxDistanceKm;
    }

    @Data
    public static class DealResponse {
        private Long id;
        private String restaurantName;
        private String cuisine;
        private String title;
        private String source;
        private String sourceUrl;
        private Double originalPrice;
        private Double discountedPrice;
        private Double discountPercent;
        private Double valueScore;
        private String city;
        private String area;
        private String scrapedAt;
        private Double savingsAmount;

        public static DealResponse from(com.fooddeals.entity.Deal deal) {
            DealResponse r = new DealResponse();
            r.id = deal.getId();
            r.restaurantName = deal.getRestaurantName();
            r.cuisine = deal.getCuisine();
            r.title = deal.getTitle();
            r.source = deal.getSource();
            r.sourceUrl = deal.getSourceUrl();
            r.originalPrice = deal.getOriginalPrice();
            r.discountedPrice = deal.getDiscountedPrice();
            r.discountPercent = deal.getDiscountPercent();
            r.valueScore = deal.getValueScore();
            r.city = deal.getCity();
            r.area = deal.getArea();
            r.scrapedAt = deal.getScrapedAt() != null ? deal.getScrapedAt().toString() : null;
            r.savingsAmount = (deal.getOriginalPrice() != null && deal.getDiscountedPrice() != null)
                    ? deal.getOriginalPrice() - deal.getDiscountedPrice() : null;
            return r;
        }
    }

    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(T data, String message) {
            ApiResponse<T> r = new ApiResponse<>();
            r.success = true;
            r.message = message;
            r.data = data;
            return r;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> r = new ApiResponse<>();
            r.success = false;
            r.message = message;
            return r;
        }
    }
}
