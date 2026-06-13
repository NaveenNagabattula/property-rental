package com.propertyrental.api.service;

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
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.repository.BookingRepository;
import com.propertyrental.api.repository.PaymentRepository;
import com.propertyrental.api.repository.PropertyRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.dto.response.PlatformConfigResponse;
import com.propertyrental.api.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl Unit Tests")
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RazorpayService razorpayService;
    @Mock private PlatformConfigService platformConfigService;
    @Mock private com.propertyrental.api.mapper.BookingMapper bookingMapper;
    @Mock private com.propertyrental.api.repository.RefundRequestRepository refundRequestRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User guest;
    private User host;
    private Property property;
    private CreateBookingRequest createRequest;
    private PlatformConfigResponse dummyConfig;

    @BeforeEach
    void setUp() {
        guest = User.builder()
                .id(UUID.randomUUID())
                .email("guest@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.GUEST)
                .build();

        host = User.builder()
                .id(UUID.randomUUID())
                .email("host@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .role(Role.HOST)
                .build();

        property = Property.builder()
                .id(UUID.randomUUID())
                .host(host)
                .title("Cozy Cabin")
                .status(PropertyStatus.ACTIVE)
                .pricePerNight(BigDecimal.valueOf(100))
                .guestCapacity(4)
                .build();

        createRequest = CreateBookingRequest.builder()
                .propertyId(property.getId())
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .guestCount(2)
                .build();

        dummyConfig = PlatformConfigResponse.builder()
                .serviceFeePercent(BigDecimal.valueOf(10))
                .taxRatePercent(BigDecimal.valueOf(18))
                .payoutDelayDays(3)
                .cancellationPolicy("FLEXIBLE")
                .build();
    }

    @Test
    @DisplayName("createBooking — success: returns BookingResponse with correct price breakdown")
    void createBooking_success() {
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));
        given(bookingRepository.existsOverlappingBooking(property.getId(), createRequest.getCheckInDate(), createRequest.getCheckOutDate()))
                .willReturn(false);
        given(userRepository.findById(guest.getId())).willReturn(Optional.of(guest));
        given(platformConfigService.getConfig()).willReturn(dummyConfig);
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });
        given(bookingMapper.toDto(any(Booking.class))).willAnswer(inv -> {
            Booking b = inv.getArgument(0);
            return BookingResponse.builder()
                    .id(b.getId())
                    .totalPrice(b.getTotalPrice())
                    .platformFee(b.getPlatformFee())
                    .status(b.getStatus().name())
                    .build();
        });

        BookingResponse response = bookingService.createBooking(createRequest, guest.getId());

        assertThat(response).isNotNull();
        // nights = 2, base = 100 * 2 = 200, fee = 10% of 200 = 20, total = 220
        assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(220));
        assertThat(response.getPlatformFee()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING.name());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking — throws BusinessRuleException when property is not active")
    void createBooking_propertyNotActive_throwsException() {
        property.setStatus(PropertyStatus.ACTIVE); // make sure it's active for dates check but then... wait
        property.setStatus(PropertyStatus.DRAFT);
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));

        assertThatThrownBy(() -> bookingService.createBooking(createRequest, guest.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available for booking");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking — throws BusinessRuleException when dates overlap")
    void createBooking_datesOverlap_throwsException() {
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));
        given(bookingRepository.existsOverlappingBooking(property.getId(), createRequest.getCheckInDate(), createRequest.getCheckOutDate()))
                .willReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(createRequest, guest.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("selected dates");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiatePayment — success: creates order and saves payment")
    void initiatePayment_success() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(220))
                .build();

        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));
        given(razorpayService.createOrder(BigDecimal.valueOf(220), bookingId.toString())).willReturn("order_123");

        PaymentOrderResponse response = bookingService.initiatePayment(bookingId, guest.getId());

        assertThat(response.getRazorpayOrderId()).isEqualTo("order_123");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("verifyPaymentAndConfirm — success: updates payment and booking status to CONFIRMED")
    void verifyPaymentAndConfirm_success() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .status(BookingStatus.PENDING)
                .build();

        Payment payment = Payment.builder()
                .razorpayOrderId("order_123")
                .status(PaymentStatus.CREATED)
                .booking(booking)
                .build();

        PaymentVerifyRequest verifyRequest = PaymentVerifyRequest.builder()
                .bookingId(bookingId)
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .razorpaySignature("signature_123")
                .build();

        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));
        given(paymentRepository.findByRazorpayOrderId("order_123")).willReturn(Optional.of(payment));
        given(razorpayService.verifySignature("order_123", "pay_123", "signature_123")).willReturn(true);
        given(bookingMapper.toDto(any(Booking.class))).willAnswer(inv -> {
            Booking b = inv.getArgument(0);
            return BookingResponse.builder()
                    .id(b.getId())
                    .status(b.getStatus().name())
                    .build();
        });

        BookingResponse response = bookingService.verifyPaymentAndConfirm(verifyRequest, guest.getId());

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED.name());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("cancelBooking — success: guest cancels and updates status to CANCELLED")
    void cancelBooking_success() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .property(property)
                .status(BookingStatus.CONFIRMED)
                .totalPrice(BigDecimal.valueOf(220))
                .build();

        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));
        given(userRepository.findById(guest.getId())).willReturn(Optional.of(guest));
        given(platformConfigService.getConfig()).willReturn(dummyConfig);
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));
        given(bookingMapper.toDto(any(Booking.class))).willAnswer(inv -> {
            Booking b = inv.getArgument(0);
            return BookingResponse.builder()
                    .id(b.getId())
                    .status(b.getStatus().name())
                    .cancellationReason(b.getCancellationReason())
                    .build();
        });

        BookingResponse response = bookingService.cancelBooking(bookingId, "Change of plans", guest.getId());

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED.name());
        assertThat(booking.getCancellationReason()).isEqualTo("Change of plans");
        verify(bookingRepository).save(booking);
        verify(refundRequestRepository).save(any(com.propertyrental.api.entity.RefundRequest.class));
    }

    @Test
    @DisplayName("cancelBooking — throws UnauthorizedException when requester is not host or guest")
    void cancelBooking_unauthorized_throwsException() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .guest(guest)
                .property(property)
                .status(BookingStatus.CONFIRMED)
                .build();

        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, "Change of plans", UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class);
    }
}
