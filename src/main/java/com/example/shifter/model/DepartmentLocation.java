package com.example.shifter.model;


import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a department location.
 * Connects a department with an address.
 */
@Entity
@Table(name = "department_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Location name (e.g. "Head Office", "Warehouse West").
     */
    @Column(nullable = false, length = 100)
    private String locationName;

    /**
     * The department this location belongs to.
     */
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * Address of the location.
     */
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;
}
