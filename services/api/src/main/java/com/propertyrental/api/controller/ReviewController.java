package com.propertyrental.api.controller;

import com.propertyrental.api.dto.request.CreateReviewRequest;
import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.ReviewResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Guest property reviews with double-blind visibility window")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * POST /api/v1/reviews
     *
     * <p>Guests submit a review for a completed booking. The review is hidden until the
     * 14-day blind window expires after checkout (enforced by the nightly scheduler).
     */
    @PostMapping
    @PreAuthorize("hasRole('GUEST')")
    @Operation(
            summary = "Submit a property review (Guest only)",
            description = "Creates a hidden review for a COMPLETED booking. "
                    + "The review becomes publicly visible 14 days after checkout.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReviewResponse response = reviewService.submitReview(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReviewResponse>builder()
                        .success(true)
                        .message("Review submitted successfully. It will become visible after the 14-day window.")
                        .data(response)
                        .build()
        );
    }

    /**
     * GET /api/v1/reviews/property/{propertyId}
     *
     * <p>Public endpoint — returns only visible reviews for the given property.
     */
    @GetMapping("/property/{propertyId}")
    @Operation(
            summary = "Get visible reviews for a property (public)",
            description = "Returns paginated, publicly visible reviews for the given property. "
                    + "Hidden reviews (still within the 14-day blind window) are excluded."
    )
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getPropertyReviews(
            @PathVariable UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewResponse> reviews = reviewService.getPropertyReviews(propertyId, page, size);
        return ResponseEntity.ok(
                ApiResponse.<Page<ReviewResponse>>builder()
                        .success(true)
                        .message("Property reviews retrieved successfully")
                        .data(reviews)
                        .build()
        );
    }

    /**
     * GET /api/v1/reviews/guest/{guestId}
     *
     * <p>Returns all reviews (visible and hidden) submitted by the given guest.
     * Accessible by the guest themselves or by SUPER_ADMIN / PROPERTY_MANAGER.
     */
    @GetMapping("/guest/{guestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROPERTY_MANAGER') or #guestId == authentication.principal.id")
    @Operation(
            summary = "Get all reviews submitted by a guest (self or admin)",
            description = "Returns all reviews the guest submitted, regardless of visibility status.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getGuestReviews(
            @PathVariable UUID guestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewResponse> reviews = reviewService.getGuestReviews(guestId, page, size);
        return ResponseEntity.ok(
                ApiResponse.<Page<ReviewResponse>>builder()
                        .success(true)
                        .message("Guest reviews retrieved successfully")
                        .data(reviews)
                        .build()
        );
    }
}
