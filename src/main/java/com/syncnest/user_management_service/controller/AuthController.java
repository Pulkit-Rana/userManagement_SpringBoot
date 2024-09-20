package com.syncnest.user_management_service.controller;

import com.syncnest.user_management_service.entity.RefreshToken;
import com.syncnest.user_management_service.exception.TokenRefreshException;
import com.syncnest.user_management_service.exception.UserAlreadyExistsException;
import com.syncnest.user_management_service.model.JwtResponseDTO;
import com.syncnest.user_management_service.model.LoginDTO;
import com.syncnest.user_management_service.model.RefreshTokenRequestDTO;
import com.syncnest.user_management_service.model.UserRegistrationDto;
import com.syncnest.user_management_service.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for managing user authentication and registration")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRegistrationService userRegistrationService;
    private final LogoutHandlerService logoutHandlerService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService, UserRegistrationService userRegistrationService, LogoutHandlerService logoutHandlerService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRegistrationService = userRegistrationService;
        this.logoutHandlerService = logoutHandlerService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user and returns a JWT token upon successful registration")
    @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = JwtResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "User already exists")
    @PostMapping("/register")
    public ResponseEntity<JwtResponseDTO> registerUser(@Valid @RequestBody UserRegistrationDto request) {
        log.warn("Received registration request: {}", request);
        try {
            JwtResponseDTO jwtResponse = userRegistrationService.registerUser(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getUsername(),
                    request.getPassword()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(jwtResponse);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "User login", description = "Authenticates the user and generates access and refresh tokens")
    @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = JwtResponseDTO.class)))
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> authenticateAndGetToken(@Valid @RequestBody LoginDTO loginDTO) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
            if (authentication.isAuthenticated()) {
                // Generate access token and refresh token
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(loginDTO.getUsername());
                JwtResponseDTO response = JwtResponseDTO.builder()
                        .accessToken(jwtService.generateToken(loginDTO.getUsername()))
                        .token(refreshToken.getToken())
                        .build();
                return ResponseEntity.ok(response);
            } else {
                throw new BadCredentialsException("Invalid credentials");
            }
        } catch (BadCredentialsException e) {
            log.error("Authentication failed for user: {}", loginDTO.getUsername(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", e);
        }
    }

    @Operation(summary = "Refresh access token", description = "Generates a new access token using a refresh token")
    @ApiResponse(responseCode = "200", description = "Access token refreshed successfully", content = @Content(schema = @Schema(implementation = JwtResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    @PostMapping("/refreshToken")
    public ResponseEntity<JwtResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshToken) {
        return refreshTokenService.findByToken(refreshToken.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> {
                    // Generate a new access token
                    String accessToken = jwtService.generateToken(userInfo.getUsername());
                    return ResponseEntity.ok(JwtResponseDTO.builder()
                            .accessToken(accessToken)
                            .token(refreshToken.getToken())
                            .build());
                })
                .orElseThrow(() -> new TokenRefreshException("Invalid or expired refresh token"));
    }

    @Operation(summary = "Logout", description = "Logs out the user, invalidates the refresh token, and blacklists the JWT")
    @ApiResponse(responseCode = "200", description = "User logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, @RequestHeader RefreshTokenRequestDTO refreshToken) {
        try {
            String result = logoutHandlerService.logout(request, refreshToken);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during logout", e);
        }
    }

    @Operation(summary = "Check login status", description = "Check if the user is authenticated")
    @ApiResponse(responseCode = "200", description = "User is authenticated")
    @GetMapping("/check")
    public ResponseEntity<String> checkLogin() {
        return ResponseEntity.ok("Hello there");
    }
}
