package com.example.guangzhouorder.service;

import com.example.guangzhouorder.entity.EmailVerificationToken;
import com.example.guangzhouorder.entity.User;
import com.example.guangzhouorder.repository.EmailVerificationTokenRepository;
import com.example.guangzhouorder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public User registerUser(String name, String email, String phone, String password, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .hashedPassword(passwordEncoder.encode(password))
                .role(role)
                .accountVerified(false)
                .loginDisabled(false)
                .build();

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .user(savedUser)
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(savedUser.getEmail(), token);
        return savedUser;
    }

    @Transactional
    public User verifyEmail(String token) {
        log.info("Attempting to verify token: {}", token);
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        log.info("Found token record, used={}, expiresAt={}", verificationToken.isUsed(), verificationToken.getExpiresAt());

        if (verificationToken.isUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
        log.info("Token marked as used, id={}", verificationToken.getId());

        User user = verificationToken.getUser();
        user.setAccountVerified(true);
        User savedUser = userRepository.save(user);
        log.info("User verified, userId={}", savedUser.getUserId());
        return savedUser;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isAccountVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .user(user)
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }
}