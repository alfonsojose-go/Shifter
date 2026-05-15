package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a physical address.
 * Can belong to a User or a Department.
 */
@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_number", length = 20)
    private String unitNumber;

    @Column(nullable = false, length = 150)
    private String street;

    /**
     * Optional complement (apartment, suite, etc.)
     */
    @Column(length = 100)
    private String complement;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 50)
    private String province;

    @Column(nullable = false, length = 50)
    private String country;

    /**
     * Address can belong to ONE user.
     */
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Address can belong to ONE department.
     */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
}

