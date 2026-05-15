package com.example.shifter.service.login;

import com.example.shifter.model.User;
import com.example.shifter.repository.UserRepository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository  userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordService passwordService;

    private User testUser;
    private final Long USER_ID = 1L;
    private final String PLAIN_PASSWORD = "password123";
    private final String HASHED_PASSWORD = "$2a$10$hashedpasswordhash";
    private final String NEW_PASSWORD = "newPassword456";


    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setPassword(HASHED_PASSWORD);
    }

    @Test
    void hashPassword_ShouldReturnEncodedPassword() {
        // Arrange
        when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(HASHED_PASSWORD);

        // Act
        String result = passwordService.hashPassword(PLAIN_PASSWORD);

        // Assert
        assertEquals(HASHED_PASSWORD, result);
        verify(passwordEncoder).encode(PLAIN_PASSWORD);
    }

    @Test
    void hashPassword_WithNullPassword_ShouldThrowIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> passwordService.hashPassword(null));

        assertEquals("Password cannot be null", exception.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void hashPassword_WithEmptyPassword_ShouldCallEncoderWithEmptyString() {
        // Arrange
        when(passwordEncoder.encode("")).thenReturn("$2a$10$emptyhash");

        // Act
        String result = passwordService.hashPassword("");

        // Assert
        assertEquals("$2a$10$emptyhash", result);
        verify(passwordEncoder).encode("");
    }

    @Test
    void updateUserPassword_WithValidUser_ShouldUpdatePassword() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("$2a$10$newhashedpassword");

        // Act
        passwordService.updateUserPassword(USER_ID, NEW_PASSWORD);

        // Assert
        assertEquals("$2a$10$newhashedpassword", testUser.getPassword());
        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserPassword_WithNonExistentUser_ShouldThrowRuntimeException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> passwordService.updateUserPassword(USER_ID, NEW_PASSWORD));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_WithNullPassword_ShouldThrowIllegalArgumentException() {
        // Arrange - NO NEED to setup userRepository mock since it won't be called

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> passwordService.updateUserPassword(USER_ID, null));

        assertEquals("New password cannot be null", exception.getMessage());

        // Verify repository is NEVER called because validation fails first
        verify(userRepository, never()).findById(anyLong());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyPassword_WhenPasswordMatches_ShouldReturnTrue() {
        // Arrange
        when(passwordEncoder.matches(PLAIN_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        // Act
        boolean result = passwordService.verifyPassword(PLAIN_PASSWORD, HASHED_PASSWORD);

        // Assert
        assertTrue(result);
        verify(passwordEncoder).matches(PLAIN_PASSWORD, HASHED_PASSWORD);
    }

    @Test
    void verifyPassword_WhenPasswordDoesNotMatch_ShouldReturnFalse() {
        // Arrange
        when(passwordEncoder.matches(PLAIN_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        // Act
        boolean result = passwordService.verifyPassword(PLAIN_PASSWORD, HASHED_PASSWORD);

        // Assert
        assertFalse(result);
        verify(passwordEncoder).matches(PLAIN_PASSWORD, HASHED_PASSWORD);
    }

    @Test
    void verifyPassword_WithNullPlainPassword_ShouldReturnFalse() {
        // Act
        boolean result = passwordService.verifyPassword(null, HASHED_PASSWORD);

        // Assert
        assertFalse(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void verifyPassword_WithNullHashedPassword_ShouldReturnFalse() {
        // Act
        boolean result = passwordService.verifyPassword(PLAIN_PASSWORD, null);

        // Assert
        assertFalse(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void verifyPassword_WithBothNull_ShouldReturnFalse() {
        // Act
        boolean result = passwordService.verifyPassword(null, null);

        // Assert
        assertFalse(result);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void verifyPassword_WithEmptyStrings_ShouldCallMatchesWithEmptyStrings() {
        // Arrange
        when(passwordEncoder.matches("", "")).thenReturn(true);

        // Act
        boolean result = passwordService.verifyPassword("", "");

        // Assert
        assertTrue(result);
        verify(passwordEncoder).matches("", "");
    }

    @Test
    void verifyPassword_WithEmptyPasswordButValidHash_ShouldCallMatches() {
        // Arrange
        when(passwordEncoder.matches("", HASHED_PASSWORD)).thenReturn(false);

        // Act
        boolean result = passwordService.verifyPassword("", HASHED_PASSWORD);

        // Assert
        assertFalse(result);
        verify(passwordEncoder).matches("", HASHED_PASSWORD);
    }

    // Additional edge case tests
    @Test
    void hashPassword_WithWhitespaceOnlyPassword_ShouldCallEncoder() {
        // Arrange
        String whitespacePassword = "   ";
        when(passwordEncoder.encode(whitespacePassword)).thenReturn("$2a$10$whitespacehash");

        // Act
        String result = passwordService.hashPassword(whitespacePassword);

        // Assert
        assertEquals("$2a$10$whitespacehash", result);
        verify(passwordEncoder).encode(whitespacePassword);
    }

    @Test
    void updateUserPassword_WithWhitespacePassword_ShouldUpdate() {
        // Arrange
        String whitespacePassword = "   ";
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(whitespacePassword)).thenReturn("$2a$10$whitespacehash");

        // Act
        passwordService.updateUserPassword(USER_ID, whitespacePassword);

        // Assert
        assertEquals("$2a$10$whitespacehash", testUser.getPassword());
        verify(passwordEncoder).encode(whitespacePassword);
    }
}