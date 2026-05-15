package com.example.shifter.service;

import com.example.shifter.dto.ShowSqueduleDTO;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.model.User;
import com.example.shifter.repository.ScheduledShiftRepository;

import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * This service works for an api to return employees scheduled
 * for a date or a range of date
 * Data Source: `scheduled_shifts` table
 **/

@Service
@RequiredArgsConstructor
public class ShowScheduleService {

    private final ScheduledShiftRepository shiftRepository;

    private final UserRepository userRepository;

    // Today
    public List<ShowSqueduleDTO> getTodaySchedule() {
        return getScheduleForDate(LocalDate.now());
    }

    // Single date
    public List<ShowSqueduleDTO> getScheduleForDate(LocalDate date) {
        return shiftRepository.findByDate(date)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Date range
    public List<ShowSqueduleDTO> getScheduleForRange(LocalDate start, LocalDate end) {
        return shiftRepository.findByDateBetween(start, end)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // This week
    public List<ShowSqueduleDTO> getScheduleForWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return getScheduleForRange(weekStart, weekEnd);
    }

    // Mapper
    private ShowSqueduleDTO toDTO(ScheduledShift shift) {
        User user = shift.getEmployee();

        return new ShowSqueduleDTO(
                user.getId(),
                user.getFullName(),
                user.getPosition() != null ? user.getPosition().getName() : null,
                shift.getDate().toString(),
                shift.getStartTime().toString(),
                shift.getEndTime().toString()
        );
    }

    /**
     * For individual employees
     */
    public Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    public List<ShowSqueduleDTO> getScheduleForEmployeeOnDate(Long userId, LocalDate date) {
        return shiftRepository.findByEmployeeIdAndDate(userId, date)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ShowSqueduleDTO> getScheduleForEmployeeWeek(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return shiftRepository.findByEmployeeIdAndDateBetween(userId, weekStart, weekEnd)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ShowSqueduleDTO> getScheduleForEmployeeRange(Long userId, LocalDate start, LocalDate end) {
        return shiftRepository.findByEmployeeIdAndDateBetween(userId, start, end)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // code for delete a shift
    public void deleteShift(Long shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new RuntimeException("Shift not found with ID: " + shiftId);
        }

        shiftRepository.deleteById(shiftId);
    }
}