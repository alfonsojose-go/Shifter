package com.example.shifter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * DTO returned to the frontend for daily budget values.
 */
@Getter
@Setter
@AllArgsConstructor
public class DailyBudgetDTO {

    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private Double budgetAmount;
}