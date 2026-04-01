package com.example.guangzhouorder.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.frontend.base-url}")
    private String appFrontendBaseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify your Guangzhou Order account");
        message.setText("Please verify your email by clicking the link below:\n\n" +
                appFrontendBaseUrl + "/verify-email?token=" + token);
        javaMailSender.send(message);
    }
}

