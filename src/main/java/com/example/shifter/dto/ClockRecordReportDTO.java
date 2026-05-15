package com.example.shifter.dto;

import com.example.shifter.enums.AttendanceStatus;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for ClockRecord Report - fields returned on frontend are mapped here.
 * This DTO is not finished
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClockRecordReportDTO {

    // Basic identifying info
    private Long userId;
    private String userName;

    // Workday info
    private LocalDate workDate;

    // Actual clock-in/out
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;

    // Scheduled shift info 
    private LocalTime  scheduledStartTime;
    private LocalTime  scheduledEndTime;

    // Calculated fields
    private Duration totalWorked;        // e.g., 7h 45m
    private Duration lateBy;             // e.g., 10 minutes late
    private Duration leftEarlyBy;        // e.g., left 15 minutes early

    // Status summary (easy for frontend)
    private AttendanceStatus attendanceStatus;

    // Status summary (easy for frontend)
    //    private String attendanceStatus;     // "ON_TIME", "LATE", "EARLY", "ABSENT", "NO_CLOCK_OUT"

    /**
     * Supports front end for
     * show cost breakdowns (horlyRate)
     * “Scheduled vs Budget” chart (scheduledCost)
     * “Actual Labor Cost” section (actualCost)
     */
    private Double hourlyWage;
    private Double scheduledCost;
    private Double actualCost;


}