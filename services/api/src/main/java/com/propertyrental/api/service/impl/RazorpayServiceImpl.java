package com.propertyrental.api.service.impl;

import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.service.RazorpayService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayServiceImpl implements RazorpayService {

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.webhook-secret:webhooksecret}")
    private String webhookSecret;

    /** Returns true when running with placeholder / dev credentials. */
    private boolean isDevMode() {
        return "rzp_test_dummykey".equals(keyId) || keyId == null || keyId.isBlank();
    }

    @Override
    public String createOrder(BigDecimal amount, String receipt) {
        // --- Dev / mock mode: skip real Razorpay call when using placeholder keys ---
        if (isDevMode()) {
            String mockOrderId = "mock_order_" + receipt.replace("-", "").substring(0, 16);
            log.warn("DEV MODE: Skipping real Razorpay order creation. Returning mock order id={}", mockOrderId);
            return mockOrderId;
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            // Razorpay expects amount in smallest currency unit (paise for INR)
            options.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            options.put("currency", "INR");
            options.put("receipt", receipt);
            options.put("payment_capture", 1);

            Order order = client.orders.create(options);
            log.info("Razorpay order created: {}", (Object) order.get("id"));
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new BusinessRuleException("Payment gateway error: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        // Dev mode: mock orders always pass signature verification
        if (orderId != null && orderId.startsWith("mock_order_")) {
            log.warn("DEV MODE: Auto-accepting signature for mock order id={}", orderId);
            return true;
        }

        try {
            String payload = orderId + "|" + paymentId;
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] computed = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return computedHex.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] computed = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return computedHex.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

}
