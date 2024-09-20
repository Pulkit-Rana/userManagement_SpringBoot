package com.syncnest.user_management_service.service;

import com.syncnest.user_management_service.model.RefreshTokenRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogoutHandlerService {

    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;


    @Autowired
    public LogoutHandlerService(TokenBlacklistService tokenBlacklistService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;

    }

    public String logout(HttpServletRequest request, RefreshTokenRequestDTO refreshToken) {

        if (request != null) {
            // Blacklist the JWT token
            tokenBlacklistService.addToBlacklist(request);
            log.info("JWT token added to blacklist");
        } else {
            log.warn("No JWT token found in request");
        }

        // Delete the refresh token if it exists
        if (refreshToken != null && refreshToken.getToken() != null) {
            refreshTokenService.deleteRefreshToken(refreshToken.getToken());
            log.info("Refresh token deleted");
        } else {
            log.warn("No refresh token provided for deletion");
        }

        // Clear the security context
        SecurityContextHolder.clearContext();

        return "Logged out successfully";
    }
}
