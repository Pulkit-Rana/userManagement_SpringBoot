package com.syncnest.user_management_service.service;

import com.syncnest.user_management_service.entity.RefreshToken;
import com.syncnest.user_management_service.entity.UserInfo;
import com.syncnest.user_management_service.exception.TokenRefreshException;
import com.syncnest.user_management_service.exception.UserNotFoundException;
import com.syncnest.user_management_service.repository.RefreshTokenRepository;
import com.syncnest.user_management_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {

    @Value("${refreshtoken.expiration.milliseconds:2592000000}") // 30 days
    private long refreshTokenDurationMs;

    @Value("${refreshtoken.max.count}")
    private int refreshTokenMaxCount;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    // Find refresh token by its value (MySQL)
    @Transactional
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Create a new refresh token for a user (MySQL)
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        UserInfo userInfo = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        // Ensure the user doesn't exceed the maximum allowed tokens
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserInfoOrderByExpiryDateDesc(userInfo);
        if (userTokens.size() >= refreshTokenMaxCount) {
            refreshTokenRepository.delete(userTokens.getLast());
            log.info("Deleted oldest refresh token with ID: {}", userTokens.getLast().getId());
        }

        // Generate a new unique refresh token
        String tokenValue = String.valueOf(UUID.randomUUID());
        RefreshToken refreshToken = RefreshToken.builder()
                .userInfo(userInfo)
                .token(tokenValue)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))  // Set expiry date
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    // Verify if a refresh token is expired, delete if expired
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            log.info("Deleting expired refresh token: {}", token.getId());
            refreshTokenRepository.delete(token);  // Delete from MySQL if expired
            throw new TokenRefreshException(token.getToken(), "Refresh token has expired. Please log in again.");
        }
        // Implement sliding window by updating the expiration date
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshTokenRepository.delete(refreshToken);
                    log.info("Deleted refresh token for user: {}", refreshToken.getUserInfo().getUsername());
                });
    }
}
