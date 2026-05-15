package com.example.shifter.dto.scheduledShift;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledShiftResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String positionName;
    private BigDecimal hourlyWage;
    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
}
