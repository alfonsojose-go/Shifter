package com.example.shifter.repository;

import com.example.shifter.model.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {
    List<AvailabilityException> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
}
