package com.propertyrental.api.controller;

import com.propertyrental.api.dto.request.PaymentVerifyRequest;
import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.BookingResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payments and Webhook validation API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/verify")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Verify Razorpay payment signature after checkout", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<BookingResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        BookingResponse response = paymentService.verifyAndConfirmPayment(request, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Payment verified and booking confirmed")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay webhook endpoint (public)", description = "Receives payment events from Razorpay. Verifies X-Razorpay-Signature header before processing.")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Webhook processed successfully")
                        .build()
        );
    }
}
