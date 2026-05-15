package com.example.shifter.util.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;


import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import io.jsonwebtoken.security.Keys;


@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String JWT_SECRET = "ThisIsAVeryStrongSecretKeyThatIsAtLeast512BitsLongWhichMeans64Characters1234567890!@#$%";
    private static final long JWT_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", JWT_EXPIRATION);
    }

    @Test
    void testGenerateToken_Success() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateToken_ContainsCorrectUsername() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("john.doe");

        String token = jwtTokenProvider.generateToken(authentication);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("john.doe", claims.getSubject());
    }

    @Test
    void testGenerateToken_ContainsExpirationTime() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtTokenProvider.generateToken(authentication);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().getTime() > claims.getIssuedAt().getTime());
    }

    @Test
    void testGenerateToken_DifferentTokensForDifferentUsers() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user1");

        String token1 = jwtTokenProvider.generateToken(authentication);

        when(userDetails.getUsername()).thenReturn("user2");
        String token2 = jwtTokenProvider.generateToken(authentication);

        assertNotEquals(token1, token2);
    }
}