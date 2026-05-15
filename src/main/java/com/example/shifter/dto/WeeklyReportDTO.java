package com.example.shifter.dto;

import lombok.*;

import java.util.List;

/**
 * Combines attendance and labor cost data into one weekly summary.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportDTO {

    private List<ClockRecordReportDTO> attendanceReport;

    private LaborCostDTO laborCost;

    private Double totalScheduledCost;
    private Double totalActualCost;
}