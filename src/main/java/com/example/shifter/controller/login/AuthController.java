package com.example.shifter.controller.login;

import com.example.shifter.dto.login.LoginRequest;
import com.example.shifter.model.User;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.util.login.JwtTokenProvider;
import com.example.shifter.util.login.TokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          TokenBlacklist tokenBlacklist,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        try {
            // 1) Authenticate user with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 2) Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3) Generate JWT token
            String token = jwtTokenProvider.generateToken(authentication);

            // 4) Extract role (e.g., "ROLE_EMPLOYEE")
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority())
                    .orElse("ROLE_EMPLOYEE");

            // 5) Look up the user so we can return their DB id
            User dbUser = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found in database"));

            Long userId = dbUser.getId();

            // Since you said employeeId == userId:
            Long employeeId = userId;

            // 6) Build response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", dbUser.getUsername());
            response.put("token", token);
            response.put("role", role);
            response.put("tokenType", "Bearer");

            // ✅ add these:
            response.put("userId", userId);
            response.put("employeeId", employeeId);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (DisabledException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Account is disabled. Please contact administrator.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Login failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String token = jwtTokenProvider.getJwtFromRequest(request);

            if (token == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "No token provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            if (!jwtTokenProvider.validateToken(token)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            String username = jwtTokenProvider.getUsernameFromToken(token);

            tokenBlacklist.blacklistToken(token);
            SecurityContextHolder.clearContext();

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "Logout successful");
            successResponse.put("username", username);
            successResponse.put("note", "Client should remove the token from storage");

            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
