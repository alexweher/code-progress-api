package com.example.codeprogress.user.service;

import com.example.codeprogress.user.User;
import com.example.codeprogress.user.dto.LoginRequest;
import com.example.codeprogress.user.dto.LoginResponse;
import com.example.codeprogress.user.exception.InvalidCredentialsException;
import com.example.codeprogress.user.repository.UserRepository;
import com.example.codeprogress.user.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void logsInWithNormalizedEmailAndCorrectPassword() {
        User user = new User("student@example.com", "stored-password-hash");
        LoginResponse token = new LoginResponse("access-token", "Bearer", 3600);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", "stored-password-hash")).thenReturn(true);
        when(jwtTokenService.issueToken(user)).thenReturn(token);

        LoginResponse response = authService.login(
                new LoginRequest("  Student@Example.COM  ", "strong-password")
        );

        assertThat(response).isEqualTo(token);
        verify(jwtTokenService).issueToken(user);
    }

    @Test
    void rejectsWrongPasswordWithoutIssuingToken() {
        User user = new User("student@example.com", "stored-password-hash");
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-password-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("student@example.com", "wrong-password")
        ))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(jwtTokenService, never()).issueToken(user);
    }

    @Test
    void rejectsUnknownEmailWithSamePublicError() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("unknown@example.com", "strong-password")
        ))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
