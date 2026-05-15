package com.example.shifter.dto.position_scheduling;

import com.example.shifter.model.scheduling.Scheduling.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO that combines schedule info with the employee's position.
 * Avoids exposing raw entities over the API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScheduleDTO {

    // --- Employee fields ---
    private Long employeeId;
    private String fullName;

    // --- Position fields (from User → Position) ---
    private String positionName;
    private BigDecimal hourlyWage;

    // --- Schedule fields ---
    private Long scheduleId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}