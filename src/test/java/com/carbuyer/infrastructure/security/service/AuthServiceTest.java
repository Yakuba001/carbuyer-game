package com.carbuyer.infrastructure.security.service;

import com.carbuyer.exception.exceptions.EmailAlreadyExistsException;
import com.carbuyer.exception.exceptions.InvalidTokenException;
import com.carbuyer.infrastructure.security.JwtService;
import com.carbuyer.infrastructure.security.dto.AuthResponse;
import com.carbuyer.infrastructure.security.dto.LoginRequest;
import com.carbuyer.infrastructure.security.dto.RefreshTokenRequest;
import com.carbuyer.infrastructure.security.dto.RegisterRequest;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserFacade userFacade;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_withExistingEmail_shouldThrowException() {
        RegisterRequest request = new RegisterRequest("user", "exist@mail.com", "password123");
        when(userFacade.existByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userFacade, never()).save(any(), any(), any());
    }

    @Test
    void register_withCorrectData_shouldEncodePasswordAndReturnTokens() {
        RegisterRequest request = new RegisterRequest("user", "exist@mail.com", "password123");
        UserDetails mockUser = mock(UserDetails.class);
        String encodedPassword = "encodedPassword";
        when(userFacade.existByEmail(request.email())).thenReturn(false);
        when(userFacade.existByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(userFacade.save(request.username(), request.email(), encodedPassword)).thenReturn(mockUser);
        when(jwtService.generateAccessToken(mockUser)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("refreshToken");

        AuthResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");

        verify(userFacade).save(request.username(), request.email(), encodedPassword);
    }

    @Test
    void login_withInvalidCredentials_shouldThrowException() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong_password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(BadCredentialsException.class);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userFacade);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_withValidCredentials_shouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest("user", "password");
        UserDetails mockUser = mock(UserDetails.class);
        when(userFacade.findByEmail(request.email())).thenReturn(mockUser);
        when(jwtService.generateAccessToken(mockUser)).thenReturn("access_token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh_token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access_token");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user", "password")
        );
    }

    @Test
    void refreshToken_whenValidToken_shouldReturnAuthResponse() {
        String userEmail = "user@email.com";
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        UserDetails mockUser = mock(UserDetails.class);
        RefreshTokenRequest request = new RefreshTokenRequest("refresh_token");
        when(jwtService.extractUsername(request.refreshToken())).thenReturn(userEmail);
        when(userFacade.findByEmail(userEmail)).thenReturn(mockUser);
        when(jwtService.isTokenValid(request.refreshToken(), mockUser)).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any())).thenReturn(refreshToken);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void refreshToken_whenTokenInvalid_shouldThrowException() {
        String userEmail = "user@email.com";
        UserDetails mockUser = mock(UserDetails.class);
        RefreshTokenRequest request = new RefreshTokenRequest("refresh_token");
        when(jwtService.extractUsername(request.refreshToken())).thenReturn(userEmail);
        when(userFacade.findByEmail(userEmail)).thenReturn(mockUser);
        when(jwtService.isTokenValid(request.refreshToken(), mockUser)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request)).isInstanceOf(InvalidTokenException.class);

        verify(jwtService, never()).generateRefreshToken(any());
        verify(jwtService, never()).generateAccessToken(any());
    }
}
