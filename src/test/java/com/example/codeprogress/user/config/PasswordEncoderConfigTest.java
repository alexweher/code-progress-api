package com.example.codeprogress.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void createsNonReversibleBcryptHash() {
        String rawPassword = "strong-password";

        String passwordHash = passwordEncoder.encode(rawPassword);

        assertThat(passwordHash).isNotEqualTo(rawPassword);
        assertThat(passwordHash).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, passwordHash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", passwordHash)).isFalse();
    }
}
