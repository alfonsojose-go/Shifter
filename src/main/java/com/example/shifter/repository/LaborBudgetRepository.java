package com.example.shifter.repository;

import com.example.shifter.model.LaborBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for daily labor budgets.
 */
public interface LaborBudgetRepository extends JpaRepository<LaborBudget, Long> {

    Optional<LaborBudget> findByDate(LocalDate date);

    List<LaborBudget> findByDateBetween(LocalDate start, LocalDate end);
}