package com.example.codeprogress.user.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void rejectsBlankIssuer() {
        assertThatThrownBy(() -> new JwtProperties("long-enough-test-secret", " ", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT issuer must be configured");
    }

    @Test
    void rejectsMissingOrNonPositiveTokenTtl() {
        assertThatThrownBy(() -> new JwtProperties("long-enough-test-secret", "codeprogress-api", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT access token TTL must be greater than zero");
        assertThatThrownBy(() -> new JwtProperties(
                "long-enough-test-secret",
                "codeprogress-api",
                Duration.ZERO
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT access token TTL must be greater than zero");
        assertThatThrownBy(() -> new JwtProperties(
                "long-enough-test-secret",
                "codeprogress-api",
                Duration.ofSeconds(-1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT access token TTL must be greater than zero");
    }
}
