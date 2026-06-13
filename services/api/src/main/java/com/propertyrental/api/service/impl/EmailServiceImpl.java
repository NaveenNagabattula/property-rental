package com.propertyrental.api.service.impl;

import com.propertyrental.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@propertyrental.com}")
    private String fromEmail;

    @Value("${APP_BASE_URL:http://localhost:5173}")
    private String baseUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) {
        String verificationLink = baseUrl + "/verify-email?token=" + verificationToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Property Rental — Verify Your Email Address");
        message.setText("""
                Hi %s,

                Welcome to Property Rental! Please verify your email address by clicking the link below:

                %s

                This link is valid for 24 hours. If you didn't create an account, you can safely ignore this email.

                Best regards,
                Property Rental Team
                """.formatted(firstName, verificationLink));

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Property Rental — Password Reset Request");
        message.setText("""
                Hi %s,

                We received a request to reset your password. Click the link below to proceed:

                %s

                This link expires in 1 hour. If you didn't request a password reset, please ignore this email.

                Best regards,
                Property Rental Team
                """.formatted(firstName, resetLink));

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendHostApplicationDecisionEmail(String toEmail, String firstName, boolean approved, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Property Rental — Host Application Decision");
        
        String decisionText = approved 
                ? "Congratulations! Your host application has been APPROVED. You can now list properties on our platform."
                : "We regret to inform you that your host application has been REJECTED.\nReason: " + reason;

        message.setText("""
                Hi %s,

                %s

                Best regards,
                Property Rental Team
                """.formatted(firstName, decisionText));

        try {
            mailSender.send(message);
            log.info("Host application decision email sent to {} (Approved: {})", toEmail, approved);
        } catch (Exception e) {
            log.error("Failed to send host application decision email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendPropertyModerationDecisionEmail(String toEmail, String hostName, String propertyTitle, boolean approved, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Property Rental — Listing Review Decision");

        String decisionText = approved
                ? "Your property listing \"" + propertyTitle + "\" has been APPROVED and is now active on the platform."
                : "Your property listing \"" + propertyTitle + "\" has been REJECTED and moved back to DRAFT.\nReason: " + reason;

        message.setText("""
                Hi %s,

                %s

                Best regards,
                Property Rental Team
                """.formatted(hostName, decisionText));

        try {
            mailSender.send(message);
            log.info("Property moderation decision email sent to {} (Approved: {})", toEmail, approved);
        } catch (Exception e) {
            log.error("Failed to send property moderation decision email to {}: {}", toEmail, e.getMessage());
        }
    }
}
