package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Represents the labor budget for a specific calendar day.
 * Each date has exactly one budget entry.
 */
@Entity
@Table(name = "labor_budget", uniqueConstraints = {
        @UniqueConstraint(columnNames = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaborBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Calendar date this budget applies to */
    @Column(nullable = false, unique = true)
    private LocalDate date;

    /** Day of week (MONDAY, TUESDAY, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    /** Budgeted labor cost for this day */
    @Column(nullable = false)
    private Double budgetAmount;
}