package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a company department (e.g. HR, IT, Operations).
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    /**
     * Primary key for the department.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Department name (must be unique).
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * One department can have many users (employees, managers).
     */
    @OneToMany(mappedBy = "department")
    private Set<User> users = new HashSet<>();

    /**
     * One department can have multiple physical locations.
     */
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<DepartmentLocation> locations = new HashSet<>();
}
