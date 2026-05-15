package com.example.shifter.service;

import com.example.shifter.dto.DailyBudgetDTO;
import com.example.shifter.model.LaborBudget;
import com.example.shifter.repository.LaborBudgetRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles creation, update, and retrieval of daily labor budgets.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final LaborBudgetRepository budgetRepository;

    /**
     * Returns the 7 daily budgets for a given week.
     * If a day has no budget yet, it is created with default 0.0.
     */
    public List<DailyBudgetDTO> getWeeklyBudget(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        // Ensure all 7 days exist
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);

            budgetRepository.findByDate(date).orElseGet(() -> {
                LaborBudget newBudget = new LaborBudget(
                        null,
                        date,
                        date.getDayOfWeek(),
                        0.0
                );
                return budgetRepository.save(newBudget);
            });
        }

        return budgetRepository.findByDateBetween(weekStart, weekEnd)
                .stream()
                .map(b -> new DailyBudgetDTO(
                        b.getDate(),
                        b.getDayOfWeek(),
                        b.getBudgetAmount()
                ))
                .toList();
    }

    /**
     * Updates or creates a budget for a specific date.
     */
    public void updateBudget(LocalDate date, Double amount) {
        LaborBudget budget = budgetRepository.findByDate(date)
                .orElse(new LaborBudget(null, date, date.getDayOfWeek(), 0.0));

        budget.setBudgetAmount(amount);
        budgetRepository.save(budget);
    }
}