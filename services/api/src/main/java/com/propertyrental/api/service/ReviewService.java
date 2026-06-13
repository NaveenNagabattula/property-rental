package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.CreateReviewRequest;
import com.propertyrental.api.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReviewService {

    /**
     * Submit a review for a completed booking.
     * The review is initially hidden (isVisible=false) under double-blind rules:
     * it only becomes public after 14 days have elapsed since checkout.
     *
     * @param request   review payload
     * @param guestId   authenticated guest submitting the review
     * @return the persisted (hidden) review DTO
     */
    ReviewResponse submitReview(CreateReviewRequest request, UUID guestId);

    /**
     * Get publicly visible reviews for a property (paginated).
     *
     * @param propertyId target property
     * @param page       zero-based page index
     * @param size       page size
     * @return page of visible ReviewResponse objects
     */
    Page<ReviewResponse> getPropertyReviews(UUID propertyId, int page, int size);

    /**
     * Get all reviews submitted by a specific guest (paginated).
     * Admin/self access only.
     *
     * @param guestId target guest
     * @param page    zero-based page index
     * @param size    page size
     * @return page of ReviewResponse objects
     */
    Page<ReviewResponse> getGuestReviews(UUID guestId, int page, int size);

    /**
     * Publish reviews whose 14-day blind window has elapsed.
     * Invoked by the scheduled job; updates isVisible=true in bulk.
     */
    void publishExpiredBlindReviews();
}
