package com.carbuyer.infrastructure.security.service;

import com.carbuyer.exception.exceptions.EmailAlreadyExistsException;
import com.carbuyer.exception.exceptions.InvalidTokenException;
import com.carbuyer.exception.exceptions.NameAlreadyExistsException;
import com.carbuyer.infrastructure.security.JwtService;
import com.carbuyer.infrastructure.security.dto.AuthResponse;
import com.carbuyer.infrastructure.security.dto.LoginRequest;
import com.carbuyer.infrastructure.security.dto.RefreshTokenRequest;
import com.carbuyer.infrastructure.security.dto.RegisterRequest;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserFacade userFacade;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userFacade.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        if (userFacade.existsByUsername(request.username())) {
            throw new NameAlreadyExistsException("Username already exists");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        UserDetails user = userFacade.save(request.username(), request.email(), encodedPassword);
        String authToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(authToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserDetails user = userFacade.findByEmail(request.email());
        String authToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(authToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String email = jwtService.extractUserEmail(request.refreshToken());
        UserDetails user = userFacade.findByEmail(email);
        boolean isTokenValid = jwtService.isTokenValid(request.refreshToken(), user);
        if (isTokenValid) {
            String authToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            return new AuthResponse(authToken, refreshToken);
        } else throw new InvalidTokenException("Invalid or expired refresh token");
    }
}
