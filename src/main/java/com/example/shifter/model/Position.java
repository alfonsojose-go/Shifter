package com.example.shifter.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a job position (e.g. Cashier, Barista, Manager).
 * Hourly wage is attached to the position.
 */
@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Position name (unique) */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** Hourly wage for this position */
    @Column(precision = 6, scale = 2)
    private BigDecimal hourlyWage;
}
