package com.example.shifter.dto;

import lombok.Data;

import java.util.Set;

/**
 * DTO used by the admin to create a new user.
 * All fields here represent INPUT from the admin.
 * The service layer handles:
 *  - Default password logic (password = email if not provided)
 *  - Default age and phone number (handled in User entity)
 */
@Data
public class CreateUserRequest {
    /** Required: full legal name of the user */
    private String fullName;
    /** Required: unique username */
    private String username;
    /** Required: unique email address */
    private String email;
    /**
     * Optional: password chosen by the admin.
     * If null or blank:
     *   The service layer will set the initial password = email.
     * If provided:
     *   The service layer will encrypt it before saving.
     */
    private String password;
    /**
     * Optional: age.
     * If not provided, the User entity applies the default (16).
     */
    private Integer age;

    /**
     * Required: list of role names.
     * Example: ["ADMIN", "EMPLOYEE"]
     * The service layer converts these Strings into Role entities.
     */
    private Set<String> roles;
}
