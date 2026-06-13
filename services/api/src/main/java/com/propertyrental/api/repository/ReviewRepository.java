package com.propertyrental.api.repository;

import com.propertyrental.api.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByPropertyIdAndIsVisible(UUID propertyId, boolean isVisible, Pageable pageable);

    Page<Review> findByGuestId(UUID guestId, Pageable pageable);

    boolean existsByBookingId(UUID bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.property.id = :propertyId AND r.isVisible = true")
    Optional<Double> findAverageRatingByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.property.id = :propertyId AND r.isVisible = true")
    long countByPropertyId(@Param("propertyId") UUID propertyId);

    /**
     * Bulk-publish reviews that are still hidden and whose booking checkout date
     * is earlier than the provided cutoff date (i.e. the 14-day blind window has elapsed).
     */
    @Modifying
    @Query("UPDATE Review r SET r.isVisible = true " +
           "WHERE r.isVisible = false AND r.booking.checkOutDate < :cutoffDate")
    int publishReviewsWithElapsedWindow(@Param("cutoffDate") LocalDate cutoffDate);
}
