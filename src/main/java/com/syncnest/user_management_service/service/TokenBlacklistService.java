package com.syncnest.user_management_service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    @Autowired
    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    // Add token to the Redis blacklist with its expiration time as TTL
    public void addToBlacklist(HttpServletRequest request) {
        String token = jwtService.extractTokenFromRequest(request);
        if (token != null) {
            String username = jwtService.extractUsername(token);
            long expiration = jwtService.extractExpiration(token).getTime();
            long ttl = expiration - System.currentTimeMillis();
            redisTemplate.opsForValue().set("blacklist:" + token, username, ttl, TimeUnit.MILLISECONDS);
            log.info("Token for user {} added to blacklist", username);
        } else {
            log.warn("No token found in request to add to blacklist");
        }
    }

    // Check if a token is blacklisted in Redis
    public Boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

}
