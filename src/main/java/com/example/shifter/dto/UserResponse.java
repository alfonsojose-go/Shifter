package com.example.shifter.dto;

import com.example.shifter.model.Department;
import lombok.Data;

import java.util.Set;

/**
 * dto for admin crud operations on users
 * Response DTO returned to the admin when viewing user information.
 * This class is intentionally simple:
 * - It exposes only the fields the admin needs to SEE.
 * - It does NOT include sensitive fields like passwords.
 * - It keeps entities (User, Role) out of controller responses.
 */
@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private Integer age;
    /**
     * Department the user belongs to.
     * NOTE: You may later replace this with a DepartmentResponse DTO
     * if you want to avoid exposing the entire Department entity.
     */
    private Department department;
    /**
     * Set of role names (e.g., ["ADMIN", "EMPLOYEE"])
     * We expose only the names, not the Role entity itself.
     */
    private Set<String> roles;
}
