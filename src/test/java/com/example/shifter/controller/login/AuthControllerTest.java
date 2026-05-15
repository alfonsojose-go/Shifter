package com.example.shifter.controller.login;


import com.example.shifter.dto.login.LoginRequest;
import com.example.shifter.util.login.JwtTokenProvider;
import com.example.shifter.util.login.TokenBlacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;

import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails; // Add this for more realistic testing

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Remove the stub from here - it causes UnnecessaryStubbingException
        // when(authentication.getPrincipal()).thenReturn("testuser");
    }

    @Test
    void testLoginSuccessful() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        
        // Create UserDetails
        UserDetails userDetails = new User(
                "testuser",
                "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        // Create authentication mock
        Authentication authentication = mock(Authentication.class);
        
        
        // Setup authenticationManager to return our authentication mock
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        // Setup jwtTokenProvider to return a token
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("mock-jwt-token");

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        
        assertNotNull(body, "Response body should not be null");
        assertEquals("Login successful", body.get("message"), "Message should match");
        assertEquals("testuser", body.get("username"), "Username should match");
        assertEquals("mock-jwt-token", body.get("token"), "Token should match");
        assertEquals("Bearer", body.get("tokenType"), "Token type should be Bearer");
        
        // Verify ALL expected interactions
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
    }

    @Test
    void testLoginWithNullUsername() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword("password123");

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Username is required", body.get("error"));
        
        // Verify no interactions with mocks
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void testLoginWithBlankUsername() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("   ");
        request.setPassword("password123");

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Username is required", body.get("error"));
        
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void testLoginWithNullPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword(null);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Password is required", body.get("error"));
        
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void testLoginWithBlankPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("   ");

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Password is required", body.get("error"));
        
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void testLoginWithInvalidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Invalid username or password", body.get("error"));
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void testLoginWithGenericException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Invalid username or password", body.get("error"));
    }
}