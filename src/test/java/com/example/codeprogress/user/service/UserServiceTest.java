package com.example.codeprogress.user.service;

import com.example.codeprogress.user.User;
import com.example.codeprogress.user.dto.RegisterUserRequest;
import com.example.codeprogress.user.dto.UserResponse;
import com.example.codeprogress.user.exception.EmailAlreadyRegisteredException;
import com.example.codeprogress.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registersUserWithNormalizedEmailAndEncodedPassword() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("$2a$10$encoded-password-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserResponse response = userService.register(
                new RegisterUserRequest("  Student@Example.COM  ", "strong-password")
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("student@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$encoded-password-hash");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("strong-password");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("student@example.com");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(passwordEncoder).encode("strong-password");
    }

    @Test
    void rejectsAlreadyRegisteredNormalizedEmail() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(
                new RegisterUserRequest("STUDENT@example.com", "strong-password")
        ))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("User with this email is already registered");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void translatesConcurrentDuplicateIntoRegistrationConflict() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("$2a$10$encoded-password-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate email constraint"));

        assertThatThrownBy(() -> userService.register(
                new RegisterUserRequest("student@example.com", "strong-password")
        ))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("User with this email is already registered");
    }
}
