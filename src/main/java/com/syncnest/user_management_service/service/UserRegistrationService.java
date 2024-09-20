package com.syncnest.user_management_service.service;

import com.syncnest.user_management_service.entity.UserInfo;
import com.syncnest.user_management_service.exception.UserAlreadyExistsException;
import com.syncnest.user_management_service.model.JwtResponseDTO;
import com.syncnest.user_management_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public UserRegistrationService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   JwtService jwtService,
                                   RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public JwtResponseDTO registerUser(String firstName, String lastName, String username, String password) {
        // Check if the user already exists by username or phone number
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("User already exists with username: " + username);
        }

        // Encrypt the password using BCrypt
        String encryptedPassword = passwordEncoder.encode(password);

        // Create the new user entity
        UserInfo newUser = UserInfo.builder()
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .password(encryptedPassword)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .build();

        // Save the new user to the database
        userRepository.save(newUser);

        // Generate JWT and refresh token for the newly registered user
        String jwtToken = jwtService.generateToken(username);
        String refreshToken = refreshTokenService.createRefreshToken(username).getToken();

        // Return the JWT and refresh token as part of the response
        return JwtResponseDTO.builder()
                .accessToken(jwtToken)
                .token(refreshToken)
                .build();
    }
}
