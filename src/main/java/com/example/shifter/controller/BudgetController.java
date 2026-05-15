package com.example.shifter.controller;

import com.example.shifter.dto.DailyBudgetDTO;
import com.example.shifter.service.BudgetService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints for managing daily labor budgets.
 */
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<DailyBudgetDTO>> getWeeklyBudget(
            @RequestParam LocalDate weekStart) {
        return ResponseEntity.ok(budgetService.getWeeklyBudget(weekStart));
    }

    @PostMapping("/update")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateBudget(
            @RequestParam LocalDate date,
            @RequestParam Double amount) {
        budgetService.updateBudget(date, amount);
        return ResponseEntity.ok("Budget updated");
    }
}