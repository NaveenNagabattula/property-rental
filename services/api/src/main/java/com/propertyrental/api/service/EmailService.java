package com.propertyrental.api.service;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String firstName, String verificationToken);

    void sendPasswordResetEmail(String toEmail, String firstName, String resetToken);

    void sendHostApplicationDecisionEmail(String toEmail, String firstName, boolean approved, String reason);

    void sendPropertyModerationDecisionEmail(String toEmail, String hostName, String propertyTitle, boolean approved, String reason);
}
