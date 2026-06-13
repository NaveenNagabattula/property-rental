package com.propertyrental.api.controller;

import com.propertyrental.api.dto.request.CancelBookingRequest;
import com.propertyrental.api.dto.request.CreateBookingRequest;
import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.BookingResponse;
import com.propertyrental.api.dto.response.PaymentOrderResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.service.BookingService;
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
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking creation, retrieval, and management")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Create a booking request", description = "GUEST only — hosts cannot book properties", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        BookingResponse response = bookingService.createBooking(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Booking created successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Get all bookings for the authenticated guest (paginated)", description = "GUEST only — returns the caller's trip history", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getGuestBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<BookingResponse> bookings = bookingService.getGuestBookings(currentUser.getId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.<Page<BookingResponse>>builder()
                        .success(true)
                        .message("Guest bookings retrieved successfully")
                        .data(bookings)
                        .build()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'PROPERTY_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Get booking detail by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        BookingResponse booking = bookingService.getBookingById(id, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Booking retrieved successfully")
                        .data(booking)
                        .build()
        );
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('GUEST', 'SUPER_ADMIN')")
    @Operation(summary = "Cancel a booking", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody CancelBookingRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        BookingResponse booking = bookingService.cancelBooking(id, request.getReason(), currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Booking cancelled successfully")
                        .data(booking)
                        .build()
        );
    }

    @PostMapping("/{id}/initiate-payment")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Initiate payment for a pending booking", description = "GUEST only", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> initiatePayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        PaymentOrderResponse response = bookingService.initiatePayment(id, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.<PaymentOrderResponse>builder()
                        .success(true)
                        .message("Payment initiated successfully")
                        .data(response)
                        .build()
        );
    }
}
