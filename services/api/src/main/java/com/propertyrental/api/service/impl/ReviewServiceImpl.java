package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.CreateReviewRequest;
import com.propertyrental.api.dto.response.ReviewResponse;
import com.propertyrental.api.entity.Booking;
import com.propertyrental.api.entity.Review;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.BookingStatus;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.mapper.ReviewMapper;
import com.propertyrental.api.repository.BookingRepository;
import com.propertyrental.api.repository.ReviewRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    /** Number of days after checkout before a review becomes publicly visible. */
    private static final int BLIND_WINDOW_DAYS = 14;

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Submit a guest review for a completed booking.
     *
     * <p>Double-blind rule: the review is persisted with {@code isVisible=false}.
     * It is only surfaced publicly after {@value #BLIND_WINDOW_DAYS} days have elapsed
     * since the booking checkout date (enforced by the nightly {@code ReviewBlindWindowScheduler}).
     *
     * <p>Business guards:
     * <ul>
     *   <li>Booking must be in {@code COMPLETED} status.</li>
     *   <li>Only the booking's own guest may submit the review.</li>
     *   <li>Each booking may only have one review (unique constraint + guard).</li>
     * </ul>
     */
    @Override
    @Transactional
    public ReviewResponse submitReview(CreateReviewRequest request, UUID guestId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + request.getBookingId()));

        if (!booking.getGuest().getId().equals(guestId)) {
            throw new UnauthorizedException("You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "Reviews can only be submitted for COMPLETED bookings; current status: "
                            + booking.getStatus());
        }

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new BusinessRuleException("A review for this booking already exists");
        }

        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + guestId));

        Review review = Review.builder()
                .booking(booking)
                .property(booking.getProperty())
                .guest(guest)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVisible(false)   // hidden until blind window expires
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review created id={} for booking={} by guest={} (hidden until blind window)",
                saved.getId(), request.getBookingId(), guestId);

        return reviewMapper.toDto(saved);
    }

    /**
     * Returns paginated, publicly visible reviews for the given property.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPropertyReviews(UUID propertyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return reviewRepository
                .findByPropertyIdAndIsVisible(propertyId, true, pageable)
                .map(reviewMapper::toDto);
    }

    /**
     * Returns all reviews submitted by the given guest (admin or self access).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getGuestReviews(UUID guestId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return reviewRepository.findByGuestId(guestId, pageable).map(reviewMapper::toDto);
    }

    /**
     * Bulk-publishes all hidden reviews whose 14-day blind window has elapsed.
     * Called by {@code ReviewBlindWindowScheduler} on a daily cron.
     */
    @Override
    @Transactional
    public void publishExpiredBlindReviews() {
        LocalDate cutoff = LocalDate.now().minusDays(BLIND_WINDOW_DAYS);
        int published = reviewRepository.publishReviewsWithElapsedWindow(cutoff);
        log.info("Blind-window publish job: {} review(s) made visible (cutoff={})", published, cutoff);
    }
}
