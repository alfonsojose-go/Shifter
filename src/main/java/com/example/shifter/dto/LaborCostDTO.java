package com.example.shifter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for returning scheduled labor cost vs budget for a full week.
 */
@Getter
@Setter
@AllArgsConstructor
public class LaborCostDTO {

    /** Map<DayOfWeek, ScheduledCost> */
    private Map<String, Double> dailyScheduledCost;

    /** Map<DayOfWeek, BudgetAmount> */
    private Map<String, Double> dailyBudget;

    private Double totalScheduled;
    private Double totalBudget;
}