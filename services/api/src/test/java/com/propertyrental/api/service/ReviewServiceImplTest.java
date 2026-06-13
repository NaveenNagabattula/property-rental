package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.CreateReviewRequest;
import com.propertyrental.api.dto.response.ReviewResponse;
import com.propertyrental.api.entity.Booking;
import com.propertyrental.api.entity.Property;
import com.propertyrental.api.entity.Review;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.BookingStatus;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.mapper.ReviewMapper;
import com.propertyrental.api.repository.BookingRepository;
import com.propertyrental.api.repository.ReviewRepository;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl Unit Tests")
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User guest;
    private User host;
    private Property property;
    private Booking completedBooking;
    private CreateReviewRequest validRequest;

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
                .title("Cosy Apartment")
                .host(host)
                .pricePerNight(BigDecimal.valueOf(1500))
                .build();

        completedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .property(property)
                .guest(guest)
                .checkInDate(LocalDate.now().minusDays(20))
                .checkOutDate(LocalDate.now().minusDays(16))
                .status(BookingStatus.COMPLETED)
                .totalPrice(BigDecimal.valueOf(6000))
                .platformFee(BigDecimal.valueOf(600))
                .guestCount(2)
                .build();

        validRequest = CreateReviewRequest.builder()
                .bookingId(completedBooking.getId())
                .rating(5)
                .comment("Excellent place to stay!")
                .build();
    }

    // ---- submitReview --------------------------------------------------------

    @Test
    @DisplayName("submitReview: happy path — creates hidden review for COMPLETED booking")
    void submitReview_success_createsHiddenReview() {
        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .booking(completedBooking)
                .property(property)
                .guest(guest)
                .rating(5)
                .comment("Excellent place to stay!")
                .isVisible(false)
                .build();

        ReviewResponse expectedDto = ReviewResponse.builder()
                .id(savedReview.getId())
                .bookingId(completedBooking.getId())
                .rating(5)
                .build();

        given(bookingRepository.findById(completedBooking.getId()))
                .willReturn(Optional.of(completedBooking));
        given(reviewRepository.existsByBookingId(completedBooking.getId()))
                .willReturn(false);
        given(userRepository.findById(guest.getId()))
                .willReturn(Optional.of(guest));
        given(reviewRepository.save(any(Review.class)))
                .willReturn(savedReview);
        given(reviewMapper.toDto(savedReview))
                .willReturn(expectedDto);

        ReviewResponse result = reviewService.submitReview(validRequest, guest.getId());

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo(completedBooking.getId());
        assertThat(result.getRating()).isEqualTo(5);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("submitReview: throws UnauthorizedException when reviewer is not the booking guest")
    void submitReview_wrongGuest_throwsUnauthorized() {
        UUID differentGuestId = UUID.randomUUID();
        given(bookingRepository.findById(completedBooking.getId()))
                .willReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> reviewService.submitReview(validRequest, differentGuestId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("own bookings");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitReview: throws BusinessRuleException when booking is not COMPLETED")
    void submitReview_bookingNotCompleted_throwsBusinessRule() {
        completedBooking.setStatus(BookingStatus.CONFIRMED);
        given(bookingRepository.findById(completedBooking.getId()))
                .willReturn(Optional.of(completedBooking));

        assertThatThrownBy(() -> reviewService.submitReview(validRequest, guest.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("COMPLETED");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitReview: throws BusinessRuleException when review already exists for the booking")
    void submitReview_duplicateReview_throwsBusinessRule() {
        given(bookingRepository.findById(completedBooking.getId()))
                .willReturn(Optional.of(completedBooking));
        given(reviewRepository.existsByBookingId(completedBooking.getId()))
                .willReturn(true);

        assertThatThrownBy(() -> reviewService.submitReview(validRequest, guest.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");

        verify(reviewRepository, never()).save(any());
    }

    // ---- getPropertyReviews --------------------------------------------------

    @Test
    @DisplayName("getPropertyReviews: returns page of visible reviews")
    void getPropertyReviews_returnsVisibleReviewsOnly() {
        Review visibleReview = Review.builder()
                .id(UUID.randomUUID())
                .property(property)
                .guest(guest)
                .booking(completedBooking)
                .rating(4)
                .isVisible(true)
                .build();

        Page<Review> reviewPage = new PageImpl<>(List.of(visibleReview));
        given(reviewRepository.findByPropertyIdAndIsVisible(
                any(UUID.class), any(Boolean.class), any(Pageable.class)))
                .willReturn(reviewPage);
        given(reviewMapper.toDto(visibleReview))
                .willReturn(ReviewResponse.builder().id(visibleReview.getId()).rating(4).build());

        Page<ReviewResponse> result = reviewService.getPropertyReviews(property.getId(), 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getRating()).isEqualTo(4);
    }

    // ---- publishExpiredBlindReviews ------------------------------------------

    @Test
    @DisplayName("publishExpiredBlindReviews: calls repository with correct cutoff date")
    void publishExpiredBlindReviews_callsRepository() {
        given(reviewRepository.publishReviewsWithElapsedWindow(any(LocalDate.class)))
                .willReturn(3);

        reviewService.publishExpiredBlindReviews();

        // Cutoff should be today minus 14 days
        LocalDate expectedCutoff = LocalDate.now().minusDays(14);
        verify(reviewRepository).publishReviewsWithElapsedWindow(expectedCutoff);
    }
}
