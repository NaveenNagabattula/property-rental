package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.PaymentVerifyRequest;
import com.propertyrental.api.dto.response.BookingResponse;

import java.util.UUID;

public interface PaymentService {

    BookingResponse verifyAndConfirmPayment(PaymentVerifyRequest request, UUID guestId);

    void handleWebhook(String payload, String signatureHeader);
}
