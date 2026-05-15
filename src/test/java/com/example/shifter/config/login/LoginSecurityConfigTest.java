package com.example.shifter.config.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import com.example.shifter.util.login.JwtAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class LoginSecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    
    private LoginSecurityConfig loginSecurityConfig;

    @BeforeEach
    void setUp() {
        loginSecurityConfig = new LoginSecurityConfig(jwtAuthenticationFilter);
    }

    @Test
    void testConstructorInitializesJwtFilter() {
        assertNotNull(loginSecurityConfig);
    }

    @Test
    void testPasswordEncoderBean() {
        BCryptPasswordEncoder encoder = loginSecurityConfig.passwordEncoder();
        assertNotNull(encoder);
        
        String password = "testPassword";
        String encoded = encoder.encode(password);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testAuthenticationManagerBean() throws Exception {
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(mockManager);
        
        AuthenticationManager manager = loginSecurityConfig.authenticationManager(authenticationConfiguration);
        assertNotNull(manager);
        assertEquals(mockManager, manager);
    }

    @Test
    void testFilterChainIsConfigured() throws Exception {
        org.springframework.security.config.annotation.web.builders.HttpSecurity httpSecurity = 
            mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class, RETURNS_DEEP_STUBS);
        
        assertDoesNotThrow(() -> loginSecurityConfig.filterChain(httpSecurity));
    }
}