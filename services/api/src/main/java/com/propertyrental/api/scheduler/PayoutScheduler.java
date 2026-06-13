package com.propertyrental.api.scheduler;

import com.propertyrental.api.entity.Booking;
import com.propertyrental.api.repository.BookingRepository;
import com.propertyrental.api.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Nightly scheduled job that identifies COMPLETED bookings eligible for host payout.
 *
 * <p>A booking becomes eligible once its checkout date is older than the platform-configured
 * {@code payoutDelayDays} (default: 3 days). This job logs payout records; actual fund
 * disbursement would be initiated via a payment-gateway reconciliation service in a
 * production implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutScheduler {

    private final BookingRepository bookingRepository;
    private final PlatformConfigService platformConfigService;

    /**
     * Runs every day at midnight (server timezone).
     * Finds all COMPLETED bookings whose checkout date is older than {@code payoutDelayDays}
     * and logs them as payout candidates.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(readOnly = true)
    public void processPayouts() {
        int payoutDelayDays = platformConfigService.getConfig().getPayoutDelayDays();
        LocalDate cutoffDate = LocalDate.now().minusDays(payoutDelayDays);

        List<Booking> eligibleBookings = bookingRepository.findCompletedBookingsEligibleForPayout(cutoffDate);

        if (eligibleBookings.isEmpty()) {
            log.info("Payout scheduler: no eligible bookings found (cutoff={})", cutoffDate);
            return;
        }

        log.info("Payout scheduler: processing {} booking(s) eligible for payout (cutoff={})",
                eligibleBookings.size(), cutoffDate);

        for (Booking booking : eligibleBookings) {
            log.info(
                    "PAYOUT_CANDIDATE bookingId={} hostId={} propertyId={} amount={} checkoutDate={}",
                    booking.getId(),
                    booking.getProperty().getHost().getId(),
                    booking.getProperty().getId(),
                    booking.getTotalPrice().subtract(booking.getPlatformFee()),
                    booking.getCheckOutDate()
            );
            // TODO: Integrate with payment-gateway payout API (e.g. Razorpay Transfers)
            //       and mark booking as PAYOUT_PROCESSED once disbursement is confirmed.
        }
    }
}
