# 🍔 Local Food Deal Finder

A Spring Boot backend that aggregates food deals from multiple sources, scores them by **actual value** (not just discount %), and sends personalised daily digests to subscribers.

---

## What makes this different from a basic project

Most food deal apps just show "50% off" and call it done. This project ranks deals by a **multi-factor value score**:

| Factor | Weight | Why |
|---|---|---|
| Rupee savings | 30% | ₹150 saved > ₹20 saved |
| Discount % | 35% | Normalized ratio |
| Affordability | 20% | Lower final price = better for most users |
| Freshness | 15% | New deals decay over 24h |

This means a 40% off on a ₹300 meal scores **higher** than a 70% off on a ₹2000 meal — which is exactly right for someone ordering lunch.

---

## Tech Stack

- **Java 21** + **Spring Boot 3.2**
- **MySQL** with **JPA / Hibernate**
- **JSoup** for web scraping
- **Spring Mail** for HTML email digest
- **@Scheduled** for automated jobs
- **Maven** build

---

## Project Structure

```
src/main/java/com/fooddeals/
├── FoodDealFinderApplication.java
├── config/
│   └── GlobalExceptionHandler.java
├── controller/
│   ├── DealController.java
│   └── SubscriberController.java
├── dto/
│   └── Dtos.java
├── entity/
│   ├── Deal.java
│   ├── Subscriber.java
│   └── DigestLog.java
├── repository/
│   ├── DealRepository.java
│   ├── SubscriberRepository.java
│   └── DigestLogRepository.java
├── scheduler/
│   └── DealScheduler.java
├── service/
│   ├── DealScraperService.java
│   ├── DealService.java
│   ├── DigestEmailService.java
│   └── SubscriberService.java
└── util/
    └── DealScoringEngine.java
```

---

## Setup & Run

### 1. Prerequisites
- Java 21
- MySQL running locally
- Maven

### 2. Configure MySQL

```sql
CREATE DATABASE fooddeals;
```

Update `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### 3. Configure Email (for digest feature)

In `application.properties`:
```properties
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_app_password
```

> **Gmail App Password**: Go to Google Account → Security → 2-Step Verification → App Passwords → Generate one for "Mail".

### 4. Run

```bash
mvn spring-boot:run
```

App starts at `http://localhost:8080`

Tables are auto-created by Hibernate on first run.

---

## API Reference

### Deals

#### Get top deals for a city
```
GET /api/deals?city=bangalore
GET /api/deals?city=bangalore&cuisine=Indian
GET /api/deals?city=bangalore&area=Koramangala
```

**Response:**
```json
{
  "success": true,
  "message": "Found 15 deals in bangalore",
  "data": [
    {
      "id": 1,
      "restaurantName": "Pizza Hut",
      "cuisine": "Pizza",
      "title": "50% off on medium pizzas above ₹299",
      "source": "Simulated",
      "originalPrice": 299.0,
      "discountedPrice": 149.0,
      "discountPercent": 50.0,
      "valueScore": 74.5,
      "savingsAmount": 150.0,
      "city": "bangalore",
      "area": "Koramangala"
    }
  ]
}
```

#### Refresh deals for a city (trigger scraper manually)
```
POST /api/deals/refresh?city=bangalore
```

#### Get deal by ID
```
GET /api/deals/{id}
```

#### Stats by source
```
GET /api/deals/stats
```

---

### Subscribers

#### Subscribe to daily digest
```
POST /api/subscribers/subscribe
Content-Type: application/json

{
  "email": "you@gmail.com",
  "city": "bangalore",
  "area": "Koramangala",
  "preferredCuisines": "Indian,Pizza",
  "maxDistanceKm": 3
}
```

#### Unsubscribe
```
GET /api/subscribers/unsubscribe?email=you@gmail.com
```

#### Trigger a test digest immediately
```
POST /api/subscribers/test-digest?email=you@gmail.com
```

---

## Scheduled Jobs

| Job | Schedule | What it does |
|---|---|---|
| Deal refresh | Every 6 hours | Scrapes fresh deals for all subscriber cities |
| Daily digest | Every day at 12pm | Sends personalised email to all active subscribers |

Change the digest time in `application.properties`:
```properties
app.digest.cron=0 0 8 * * *   # 8am instead of noon
```

---

## Quick Demo Flow

```bash
# 1. Refresh deals for Bangalore
curl -X POST "http://localhost:8080/api/deals/refresh?city=bangalore"

# 2. See top deals (sorted by value score)
curl "http://localhost:8080/api/deals?city=bangalore"

# 3. Filter by cuisine
curl "http://localhost:8080/api/deals?city=bangalore&cuisine=Indian"

# 4. Subscribe to daily digest
curl -X POST http://localhost:8080/api/subscribers/subscribe \
  -H "Content-Type: application/json" \
  -d '{"email":"you@gmail.com","city":"bangalore","preferredCuisines":"Indian,Pizza"}'

# 5. Trigger a test digest right now
curl -X POST "http://localhost:8080/api/subscribers/test-digest?email=you@gmail.com"

# 6. See scraper stats
curl "http://localhost:8080/api/deals/stats"
```

---




