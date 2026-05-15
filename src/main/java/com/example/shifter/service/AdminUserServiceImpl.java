package com.example.shifter.service;

import com.example.shifter.dto.CreateUserRequest;
import com.example.shifter.dto.UpdateUserRequest;
import com.example.shifter.dto.UserResponse;
import com.example.shifter.model.Role;
import com.example.shifter.model.User;
import com.example.shifter.repository.RoleRepository;
import com.example.shifter.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service used exclusively by ADMIN users to manage application users.
 * Handles:
 *  - Creating users
 *  - Updating users
 *  - Deleting users
 *  - Fetching users
 * This service NEVER exposes the User entity directly to controllers.
 * All responses are mapped to UserResponse DTOs.
 */
@Service // Marks this class as a Spring-managed service
@RequiredArgsConstructor // Lombok: creates constructor for final fields
@PreAuthorize("hasRole('ADMIN')") //All methods in this service can only be executed by an admin user
public class AdminUserServiceImpl implements AdminUserService {

    // Repository to access users table
    private final UserRepository userRepository;

    // Repository to access roles table
    private final RoleRepository roleRepository;

    // Used to encrypt passwords before saving them
    private final BCryptPasswordEncoder passwordEncoder; //changed this to BCryptPasswordEncoder

    /**
     * Creates a new user with roles.
     * This method is used by ADMIN only.
     * - If admin provides a password → encode it
     * - If not → use email as the initial password
     */
    @Override
    public UserResponse createUser(CreateUserRequest request) {

        // Check if username already exists to avoid duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Determine password:
        // If admin provided one → use it
        // If not → default to email
        String rawPassword = (request.getPassword() == null || request.getPassword().isBlank())
                ? request.getEmail()
                : request.getPassword();

        // Encrypt password
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Create user with minimal constructor
        User user = new User(
                request.getFullName(),
                request.getUsername(),
                encodedPassword,
                request.getEmail()
        );

        // If admin provided age, override default (16)
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }

        // Convert role names (String) into Role entities
        Set<Role> roles = request.getRoles().stream()
                .map(roleName ->
                        roleRepository.findByName(roleName)
                                .orElseThrow(() ->
                                        new RuntimeException("Role not found: " + roleName)
                                )
                )
                .collect(Collectors.toSet());

        // Assign roles to user
        roles.forEach(user::addRole);

        // Save user to database
        userRepository.save(user);

        // Convert entity to response DTO
        return mapToResponse(user);
    }

    /**
     * Returns/Fetch a single user by ID.
     */
    @Override
    public UserResponse getUserById(Long id) {

        // Find user or throw error if not found
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToResponse(user);
    }

    /**
     * Returns/Fetch all users in the system.
     */
    @Override
    public List<UserResponse> getAllUsers() {

        // Fetch all users and convert them to response DTOs
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Updates user information and/or roles.
     * Only updates fields that are provided in the request.
     */
    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        // Load user from database
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields only if they were provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }

        // Update roles if provided
        if (request.getRoles() != null) {
            user.getRoles().clear();

            Set<Role> roles = request.getRoles().stream()
                    .map(roleName ->
                            roleRepository.findByName(roleName)
                                    .orElseThrow(() ->
                                            new RuntimeException("Role not found: " + roleName)
                                    )
                    )
                    .collect(Collectors.toSet());

            roles.forEach(user::addRole);
        }

        userRepository.save(user);
        return mapToResponse(user);
    }

    /**
     * Deletes a user by ID.
     */
    @Override
    public void deleteUser(Long id) {

        // Check existence before deleting
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }

        userRepository.deleteById(id);
    }

    /**
     * Helper method to convert User entity into UserResponse DTO.
     * Keeps entities away from controllers response.
     */
    private UserResponse mapToResponse(User user) {

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());

        // Convert Role entities into role names
        response.setRoles(
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );

        return response;
    }
}
