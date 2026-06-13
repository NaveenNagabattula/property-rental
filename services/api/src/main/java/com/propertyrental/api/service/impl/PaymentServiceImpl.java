package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.PaymentVerifyRequest;
import com.propertyrental.api.dto.response.BookingResponse;
import com.propertyrental.api.entity.Booking;
import com.propertyrental.api.entity.Payment;
import com.propertyrental.api.entity.enums.BookingStatus;
import com.propertyrental.api.entity.enums.PaymentStatus;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.repository.BookingRepository;
import com.propertyrental.api.repository.PaymentRepository;
import com.propertyrental.api.service.BookingService;
import com.propertyrental.api.service.PaymentService;
import com.propertyrental.api.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;

    @Override
    @Transactional
    public BookingResponse verifyAndConfirmPayment(PaymentVerifyRequest request, UUID guestId) {
        return bookingService.verifyPaymentAndConfirm(request, guestId);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        log.info("Received Razorpay Webhook notification");

        boolean isValid = razorpayService.verifyWebhookSignature(payload, signatureHeader);
        if (!isValid) {
            log.error("Webhook signature verification failed");
            throw new BusinessRuleException("Webhook signature verification failed");
        }

        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event");
            log.info("Razorpay Webhook Event: {}", event);

            if ("order.paid".equals(event) || "payment.captured".equals(event)) {
                JSONObject payloadObj = json.getJSONObject("payload");
                
                String razorpayOrderId = null;
                String razorpayPaymentId = null;

                if (payloadObj.has("payment")) {
                    JSONObject paymentObj = payloadObj.getJSONObject("payment").getJSONObject("entity");
                    razorpayPaymentId = paymentObj.optString("id");
                    razorpayOrderId = paymentObj.optString("order_id");
                }
                
                if ((razorpayOrderId == null || razorpayOrderId.isBlank()) && payloadObj.has("order")) {
                    JSONObject orderObj = payloadObj.getJSONObject("order").getJSONObject("entity");
                    razorpayOrderId = orderObj.optString("id");
                }

                if (razorpayOrderId != null && !razorpayOrderId.isBlank()) {
                    Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
                    if (payment != null && payment.getStatus() != PaymentStatus.CAPTURED) {
                        payment.setStatus(PaymentStatus.CAPTURED);
                        if (razorpayPaymentId != null) {
                            payment.setRazorpayPaymentId(razorpayPaymentId);
                        }
                        paymentRepository.save(payment);

                        Booking booking = payment.getBooking();
                        if (booking.getStatus() == BookingStatus.PENDING) {
                            booking.setStatus(BookingStatus.CONFIRMED);
                            bookingRepository.save(booking);
                            log.info("Booking status updated to CONFIRMED via webhook for orderId={}", razorpayOrderId);
                        }
                    } else {
                        log.info("Payment not found or already processed for orderId={}", razorpayOrderId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Razorpay webhook payload", e);
            throw new BusinessRuleException("Error processing webhook: " + e.getMessage());
        }
    }
}
