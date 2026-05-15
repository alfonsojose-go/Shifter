package com.example.shifter.controller.users;

import com.example.shifter.dto.UpdatePasswordDTO;
import com.example.shifter.service.UserService;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.dto.availability.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("api/users")
@Validated
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/{username}/employee-id")
    public ResponseEntity<ApiResponse<Long>> getEmployeeId(@PathVariable String username) {
        log.info("GET /api/users/{}/employee-id - Getting employee ID for username", username);

        try {
            return userRepository.findByUsername(username)
                    .map(user -> ResponseEntity.ok(ApiResponse.success(
                            "Employee ID retrieved successfully",
                            user.getId())))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("User not found with username: " + username)));

        } catch (Exception e) {
            log.error("Error getting employee ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve employee ID"));
        }
    }

    /**
     * Samara - Allows a logged-in user to update their password.
     */
    private final UserService userService;

    @PutMapping("/update-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePassword(
            @RequestParam Long userId,
            @RequestBody UpdatePasswordDTO dto) {

        userService.updatePassword(userId, dto);
        return ResponseEntity.ok("Password updated successfully");
    }
}