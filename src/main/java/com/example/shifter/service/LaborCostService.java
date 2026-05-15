package com.example.shifter.service;

import com.example.shifter.dto.LaborCostDTO;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.model.LaborBudget;
import com.example.shifter.repository.ScheduledShiftRepository;
import com.example.shifter.repository.LaborBudgetRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes scheduled labor cost for each day of a week.
 * Uses hourlyWage from Position table.
 */
@Service
@RequiredArgsConstructor
public class LaborCostService {

    private final ScheduledShiftRepository shiftRepository;
    private final LaborBudgetRepository budgetRepository;

    public LaborCostDTO getWeeklyLaborCost(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        Map<String, Double> scheduledCostMap = new HashMap<>();
        Map<String, Double> budgetMap = new HashMap<>();

        double totalScheduled = 0.0;
        double totalBudget = 0.0;

        // Fetch all shifts for the week
        List<ScheduledShift> shifts = shiftRepository.findByDateBetween(weekStart, weekEnd);

        // Fetch all budgets for the week
        List<LaborBudget> budgets = budgetRepository.findByDateBetween(weekStart, weekEnd);

        // Initialize maps for all 7 days
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            String day = date.getDayOfWeek().name();

            scheduledCostMap.put(day, 0.0);
            budgetMap.put(day, 0.0);
        }

        // Fill budget map
        for (LaborBudget b : budgets) {
            String day = b.getDayOfWeek().name();
            budgetMap.put(day, b.getBudgetAmount());
            totalBudget += b.getBudgetAmount();
        }

        // Compute scheduled cost
        for (ScheduledShift shift : shifts) {
            LocalDate date = shift.getDate();
            String day = date.getDayOfWeek().name();

            // Get hourly wage from Position
//            BigDecimal wage = shift.getEmployee().getPosition().getHourlyWage();
//            double hourlyRate = wage != null ? wage.doubleValue() : 0.0;

            double hourlyRate = 0.0;
            if (shift.getEmployee() != null &&
                    shift.getEmployee().getPosition() != null &&
                    shift.getEmployee().getPosition().getHourlyWage() != null) {
                hourlyRate = shift.getEmployee().getPosition().getHourlyWage().doubleValue();
            }

            // Calculate hours
            Duration duration = Duration.between(shift.getStartTime(), shift.getEndTime());
            double hours = duration.toMinutes() / 60.0;

            double cost = hours * hourlyRate;

            scheduledCostMap.put(day, scheduledCostMap.get(day) + cost);
            totalScheduled += cost;
        }

        return new LaborCostDTO(
                scheduledCostMap,
                budgetMap,
                totalScheduled,
                totalBudget
        );
    }
}