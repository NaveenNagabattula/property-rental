package com.propertyrental.api.scheduler;

import com.propertyrental.api.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly scheduled job that lifts the double-blind hold on reviews.
 *
 * <p>When a guest submits a review, it is stored with {@code isVisible=false}
 * to implement the double-blind policy: neither party sees the other's review
 * until the 14-day window after checkout has elapsed. This scheduler runs at
 * 01:00 every day and delegates to {@link ReviewService#publishExpiredBlindReviews()}
 * to bulk-update eligible reviews to visible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewBlindWindowScheduler {

    private final ReviewService reviewService;

    /**
     * Runs every day at 01:00 (server timezone).
     * Reviews whose booking checkout date is more than 14 days ago are made visible.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void publishBlindWindowExpiredReviews() {
        log.info("ReviewBlindWindowScheduler: starting blind-window expiry check");
        reviewService.publishExpiredBlindReviews();
    }
}
