package com.propertyrental.api.repository;

import com.propertyrental.api.entity.Booking;
import com.propertyrental.api.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByGuestId(UUID guestId, Pageable pageable);

    Page<Booking> findByPropertyHostId(UUID hostId, Pageable pageable);

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.property.id = :propertyId
            AND b.status NOT IN ('CANCELLED', 'REJECTED')
            AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
            """)
    boolean existsOverlappingBooking(
            @Param("propertyId") UUID propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    List<Booking> findByPropertyIdAndStatusIn(UUID propertyId, List<BookingStatus> statuses);

    /**
     * Find COMPLETED bookings whose checkout date is before the given cutoff,
     * used by the payout scheduler to identify bookings eligible for host payout.
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'COMPLETED' AND b.checkOutDate < :cutoffDate")
    List<Booking> findCompletedBookingsEligibleForPayout(@Param("cutoffDate") LocalDate cutoffDate);
}

