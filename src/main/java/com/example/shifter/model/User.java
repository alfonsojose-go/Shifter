package com.example.shifter.model;

import com.example.shifter.model.Department;
import com.example.shifter.model.Address;
import com.example.shifter.model.Position;
import com.example.shifter.model.ContractType;
import com.example.shifter.model.Skill;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an application user.
 * This entity is managed by JPA and stored in the "users" table.
 */

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        }
)
@Getter
@Setter
//@NoArgsConstructor // JPA requires a no-args constructor
@ToString(exclude = "roles") //prevents infinite recursion when printing and lazy-loading issues
public class User {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full legal name of the user */
    @Column(nullable = false, length = 100)
    private String fullName;

    /** Unique username used for login */
    @Column (nullable = false, length = 50)
    private String username;

    /**
     * Encrypted password.
     * NOTE: The initial password is set in the service layer.
     */
    @Column (nullable = false, length = 100) //store encrypted passwords
    private String password;

    /** Unique email address */
    @Column (nullable = false, length = 50)
    private String email;

    /**
     * North American phone number.
     * Strict NANP validation:
     * - Optional +1
     * - Area code 2–9
     * - Prefix 2–9
     * - 10 digits total after removing formatting
     */
    @Pattern(
            regexp = "^(\\+?1[-. ]?)?\\(?[2-9][0-9]{2}\\)?[-. ]?[2-9][0-9]{2}[-. ]?[0-9]{4}$",
            message = "Invalid North American phone number"
    )
    @Column(name = "phone_number", nullable = false, length = 14) //14 = north america phone length
    private String phoneNumber;

    /** User age (required field) */
    @Max(value = 99)
    @Column (nullable = false)
    private Integer age;

    /**
     * Security roles (ADMIN, MANAGER, EMPLOYEE).
     * Used by Spring Security & JWT.
     * A user can have multiple roles.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Each user belongs to one department.
     */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /**
     * Each user can have one address.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Address address;

    /**
     * User position (nullable).
     */
    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;

    /**
     * User contract type (nullable).
     */
    @ManyToOne
    @JoinColumn(name = "contract_type_id")
    private ContractType contractType;

    /**
     * User skills (nullable).
     */
    @ManyToMany
    @JoinTable(
            name = "user_skills",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    /**
     * Deletes scheduledShifts rows when a user is deleted
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<ScheduledShift> scheduledShifts = new HashSet<>();

    /**
     * Deletes availabilityExceptions rows when a user is deleted
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<AvailabilityException> availabilityExceptions = new HashSet<>();

    /**
     * Deletes clockRecords rows when a user is deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<ClockRecord> clockRecords = new HashSet<>();

    /**
     * Default values applied to every new User instance.
     * JPA ALWAYS uses the no-args constructor, so this is the safest place.
     */
    public User() {
        this.age = 16;                        // Default minimum age
        this.phoneNumber = "(222) 222-2222";  // Valid NANP placeholder
    }

    /**
     * Convenience constructor for required fields.
     * Does NOT include age or phone number — defaults handle those.
     */
    public User(String fullName, String username, String password, String email) {
        this(); // Apply default values first
        this.fullName = fullName;
        this.username = username;
        this.password = password; // Password is already encoded in the service layer
        this.email = email;
    }

    /** Add a role to the user
     * Helper methods for role management
     * */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /** Remove a role from the user */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * Automatically formats phone numbers into (AAA) BBB-CCCC.
     * Accepts many input formats and normalizes them.
     */
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            this.phoneNumber = null;
            return;
        }

        // Remove all non-digit characters
        String digits = phoneNumber.replaceAll("\\D", "");

        // Remove leading country code "1"
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }

        // Format only if valid 10-digit NANP number
        if (digits.length() == 10) {
            this.phoneNumber = String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6)
            );
        } else {
            // Fallback: store raw input (validation will catch invalid cases)
            this.phoneNumber = phoneNumber;
        }
    }

}
