package com.propertyrental.api.service;

public interface RazorpayService {

    String createOrder(java.math.BigDecimal amount, String receipt);

    boolean verifySignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String payload, String signature);

}
