package com.syncnest.user_management_service.service;

import com.syncnest.user_management_service.entity.UserInfo;
import com.syncnest.user_management_service.model.UserAccessDetails;
import com.syncnest.user_management_service.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    UserRepository userRepository;
    TokenBlacklistService tokenBlacklistService;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository, TokenBlacklistService tokenBlacklistService, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Entering in loadUserByUsername Method...");
        UserInfo user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        logger.info("User Authenticated Successfully..!!!");
        return new UserAccessDetails(user);
    }
}
