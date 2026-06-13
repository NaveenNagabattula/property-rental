package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.CreateBookingRequest;
import com.propertyrental.api.dto.request.PaymentVerifyRequest;
import com.propertyrental.api.dto.response.BookingResponse;
import com.propertyrental.api.dto.response.PaymentOrderResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, UUID guestId);

    PaymentOrderResponse initiatePayment(UUID bookingId, UUID guestId);

    BookingResponse verifyPaymentAndConfirm(PaymentVerifyRequest request, UUID guestId);

    BookingResponse cancelBooking(UUID bookingId, String reason, UUID requesterId);

    Page<BookingResponse> getGuestBookings(UUID guestId, int page, int size);

    Page<BookingResponse> getHostBookings(UUID hostId, int page, int size);

    BookingResponse getBookingById(UUID bookingId, UUID requesterId);
}
