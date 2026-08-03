package com.example.nutritionsporttracker.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET_ONE = encodeSecret(
            "01234567890123456789012345678901"
    );

    private static final String SECRET_TWO = encodeSecret(
            "abcdefghijklmnopqrstuvwxyz123456"
    );

    @Test
    void shouldGenerateValidTokenAndExtractUsername() {
        JwtTokenProvider provider =
                createProvider(3_600_000, SECRET_ONE);

        String token =
                provider.generateToken("meral@example.com");

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals(
                "meral@example.com",
                provider.getUsernameFromToken(token)
        );
    }

    @Test
    void shouldRejectMalformedToken() {
        JwtTokenProvider provider =
                createProvider(3_600_000, SECRET_ONE);

        assertFalse(
                provider.validateToken("not-a-valid-jwt")
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtTokenProvider firstProvider =
                createProvider(3_600_000, SECRET_ONE);
        JwtTokenProvider secondProvider =
                createProvider(3_600_000, SECRET_TWO);

        String token =
                firstProvider.generateToken("meral@example.com");

        assertFalse(secondProvider.validateToken(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtTokenProvider provider =
                createProvider(-1_000, SECRET_ONE);

        String token =
                provider.generateToken("meral@example.com");

        assertFalse(provider.validateToken(token));
    }

    private JwtTokenProvider createProvider(
            long expiration,
            String secret
    ) {
        JwtTokenProvider provider = new JwtTokenProvider();

        ReflectionTestUtils.setField(
                provider,
                "jwtSecret",
                secret
        );
        ReflectionTestUtils.setField(
                provider,
                "jwtExpirationMs",
                expiration
        );

        provider.init();
        return provider;
    }

    private static String encodeSecret(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
