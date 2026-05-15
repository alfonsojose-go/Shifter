package com.example.shifter.dto;

import lombok.Data;

import java.util.Set;

/**
 * DTO used by the admin to update an existing user.
 * All fields are OPTIONAL because this DTO represents a "partial update".
 * The service layer checks each field and updates only the ones provided.
 */
@Data
public class UpdateUserRequest {
    /** Optional: update the user's full name */
    private String fullName;

    /** Optional: update the user's email */
    private String email;

    /**
     * Optional: update the user's roles.
     * Example: ["ADMIN", "EMPLOYEE"]
     * The service layer converts these Strings into Role entities.
     */
    private Set<String> roles;

    /**
     * Optional: update the user's password.
     * If provided, the service layer encrypts it before saving.
     */
    private String password;

    /** Optional: update the username */
    private String username;

    /** Optional: update the user's age */
    private Integer age;

    /** Optional: update the user's phone number */
    private String phoneNumber;
}
