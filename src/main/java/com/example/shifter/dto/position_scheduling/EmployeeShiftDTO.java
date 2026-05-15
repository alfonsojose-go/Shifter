package com.example.shifter.dto.position_scheduling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO that combines scheduled shift info with the employee's position.
 * Uses java.time.DayOfWeek (derived automatically from date in the entity).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeShiftDTO {

    // --- Employee fields ---
    private Long employeeId;
    private String fullName;

    // --- Position fields (from User → Position, nullable) ---
    private String positionName;
    private BigDecimal hourlyWage;

    // --- Shift fields ---
    private Long shiftId;
    private LocalDate date;
    private DayOfWeek dayOfWeek;   // derived from date via @PrePersist/@PreUpdate
    private LocalTime startTime;
    private LocalTime endTime;
}