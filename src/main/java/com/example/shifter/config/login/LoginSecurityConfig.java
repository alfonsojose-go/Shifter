package com.example.shifter.config.login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // Added by Laarni Cerna
import org.springframework.web.cors.CorsConfigurationSource; // Added by Laarni Cerna
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Added by Laarni Cerna
import java.util.Arrays; // Added by Laarni Cerna

import com.example.shifter.util.login.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity // Enables @PreAuthorize
public class LoginSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public LoginSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. ENABLE CORS HERE
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 1. Added by Laarni
                // End

                .csrf(csrf -> csrf.disable()) // Disable CSRF for API
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/login").permitAll() // Allow login without authentication
                        .requestMatchers("/api/auth/logout").permitAll() // Ensure logout is accessible if needed
                        .requestMatchers("/").permitAll() // Allow access to root path
                        .requestMatchers(
                            "/index.html",
                            "/swagger-ui/**", 
                            "/v3/api-docs*/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**",
                            "/swagger-ui.html",
                            "/configuration/ui",
                            "/configuration/security",
                            "/favicon.ico",
                            "/error"
                        ).permitAll() // Allow access to index.html

                        // availability endpoints
                        .requestMatchers(HttpMethod.GET, "/api/employee/availabilities/**").hasAnyAuthority("EMPLOYEE", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/employee/availabilities/**").hasAuthority("EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/employee/availabilities/**").hasAuthority("EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/employee/availabilities/**").hasAuthority("EMPLOYEE")

                        // scheduling endpoints
                        .requestMatchers(HttpMethod.GET, "/api/scheduling/**").hasAnyAuthority("MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/api/scheduling/**").hasAuthority("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/scheduling/**").hasAuthority("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/scheduling/**").hasAuthority("MANAGER")

                        // requests endpoints
                        .requestMatchers(HttpMethod.GET, "/api/requests/**").hasAnyAuthority("EMPLOYEE", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/requests/**").hasAuthority("EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/requests/**").hasAnyAuthority("EMPLOYEE", "MANAGER")

                        // role protected pages
                        .requestMatchers("/user/**").hasAuthority("EMPLOYEE")
                        .requestMatchers("/mgr/**").hasAuthority("MANAGER")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")

                        .anyRequest().authenticated() // All other endpoints require authentication
                )
                .formLogin(form -> form.disable()) // Disable default form login
                .httpBasic(httpBasic -> httpBasic.disable()) // Disable basic auth
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. DEFINE THE CORS CONFIGURATION BEAN: Added by Laarni Cerna
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow the React Frontend URL (Port 5173)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));

        // Allow standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow headers typically used in APIs
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));

        // Allow credentials (cookies/auth headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    // End

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    @Primary
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
