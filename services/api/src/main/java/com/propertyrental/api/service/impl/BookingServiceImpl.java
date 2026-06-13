package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.CreateBookingRequest;
import com.propertyrental.api.dto.request.PaymentVerifyRequest;
import com.propertyrental.api.dto.response.BookingResponse;
import com.propertyrental.api.dto.response.PaymentOrderResponse;
import com.propertyrental.api.entity.Booking;
import com.propertyrental.api.entity.Payment;
import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.BookingStatus;
import com.propertyrental.api.entity.enums.PaymentStatus;
import com.propertyrental.api.entity.enums.PropertyStatus;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.ResourceNotFoundException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.repository.BookingRepository;
import com.propertyrental.api.repository.PaymentRepository;
import com.propertyrental.api.repository.PropertyRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.BookingService;
import com.propertyrental.api.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final com.propertyrental.api.service.PlatformConfigService platformConfigService;
    private final com.propertyrental.api.mapper.BookingMapper bookingMapper;
    private final com.propertyrental.api.repository.RefundRequestRepository refundRequestRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, UUID guestId) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + request.getPropertyId()));

        if (property.getStatus() != PropertyStatus.ACTIVE) {
            throw new BusinessRuleException("Property is not available for booking");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Check-in date cannot be in the past");
        }

        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BusinessRuleException("Check-out date must be after check-in date");
        }

        if (request.getGuestCount() > property.getGuestCapacity()) {
            throw new BusinessRuleException("Guest count exceeds property capacity of " + property.getGuestCapacity());
        }

        if (bookingRepository.existsOverlappingBooking(property.getId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new BusinessRuleException("Property is not available for the selected dates");
        }

        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + guestId));

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal baseAmount = property.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        
        BigDecimal platformFeePercentage = platformConfigService.getConfig().getServiceFeePercent();
        BigDecimal platformFee = baseAmount
                .multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = baseAmount.add(platformFee);

        Booking booking = Booking.builder()
                .property(property)
                .guest(guest)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .guestCount(request.getGuestCount())
                .totalPrice(totalPrice)
                .platformFee(platformFee)
                .status(BookingStatus.PENDING)
                .specialRequests(request.getSpecialRequests())
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created id={} for guest={} property={}", saved.getId(), guestId, property.getId());
        return bookingMapper.toDto(saved);
    }

    @Override
    @Transactional
    public PaymentOrderResponse initiatePayment(UUID bookingId, UUID guestId) {
        Booking booking = getBookingForGuest(bookingId, guestId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("Payment can only be initiated for PENDING bookings");
        }

        String razorpayOrderId = razorpayService.createOrder(booking.getTotalPrice(), booking.getId().toString());

        Payment payment = Payment.builder()
                .booking(booking)
                .razorpayOrderId(razorpayOrderId)
                .amount(booking.getTotalPrice())
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .build();

        paymentRepository.save(payment);

        return PaymentOrderResponse.builder()
                .bookingId(bookingId)
                .razorpayOrderId(razorpayOrderId)
                .amount(booking.getTotalPrice())
                .currency("INR")
                .status("CREATED")
                .build();
    }

    @Override
    @Transactional
    public BookingResponse verifyPaymentAndConfirm(PaymentVerifyRequest request, UUID guestId) {
        Booking booking = getBookingForGuest(request.getBookingId(), guestId);

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        boolean isValid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            throw new BusinessRuleException("Payment signature verification failed");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Payment confirmed for booking={}", booking.getId());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, String reason, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        boolean isGuest = booking.getGuest().getId().equals(requesterId);
        boolean isHost = booking.getProperty().getHost().getId().equals(requesterId);

        if (!isGuest && !isHost) {
            throw new UnauthorizedException("You are not authorised to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        Booking savedBooking = bookingRepository.save(booking);

        if (oldStatus == BookingStatus.CONFIRMED) {
            User requester = userRepository.findById(requesterId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterId));

            String policy = platformConfigService.getConfig().getCancellationPolicy();
            BigDecimal refundAmount = BigDecimal.ZERO;
            if ("FLEXIBLE".equalsIgnoreCase(policy)) {
                refundAmount = booking.getTotalPrice();
            } else if ("MODERATE".equalsIgnoreCase(policy)) {
                refundAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
            }

            com.propertyrental.api.entity.RefundRequest refundRequest = com.propertyrental.api.entity.RefundRequest.builder()
                    .booking(booking)
                    .requestedBy(requester)
                    .reason(reason)
                    .status("PENDING")
                    .refundAmount(refundAmount)
                    .build();

            refundRequestRepository.save(refundRequest);
            log.info("Refund request of {} queued for booking {} cancellation by user={}", refundAmount, bookingId, requesterId);
        }

        log.info("Booking {} cancelled by user={}", bookingId, requesterId);
        return bookingMapper.toDto(savedBooking);
    }

    @Override
    public Page<BookingResponse> getGuestBookings(UUID guestId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return bookingRepository.findByGuestId(guestId, pageable).map(bookingMapper::toDto);
    }

    @Override
    public Page<BookingResponse> getHostBookings(UUID hostId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return bookingRepository.findByPropertyHostId(hostId, pageable).map(bookingMapper::toDto);
    }

    @Override
    public BookingResponse getBookingById(UUID bookingId, UUID requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        boolean isGuest = booking.getGuest().getId().equals(requesterId);
        boolean isHost = booking.getProperty().getHost().getId().equals(requesterId);
        if (!isGuest && !isHost) {
            throw new UnauthorizedException("Access denied");
        }
        return bookingMapper.toDto(booking);
    }

    private Booking getBookingForGuest(UUID bookingId, UUID guestId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getGuest().getId().equals(guestId)) {
            throw new UnauthorizedException("This booking does not belong to you");
        }
        return booking;
    }

}
