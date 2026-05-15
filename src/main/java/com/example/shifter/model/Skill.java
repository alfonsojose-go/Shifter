package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a skill that a user can have.
 * Example: Cashier, Barista, Inventory.
 */
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Skill name */
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
