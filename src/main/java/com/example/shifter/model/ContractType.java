package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents employment contract type
 * (e.g. Full-Time, Part-Time, Contractor).
 */
@Entity
@Table(name = "contract_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Contract type name */
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
