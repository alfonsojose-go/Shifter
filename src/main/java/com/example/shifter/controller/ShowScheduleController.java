package com.example.shifter.controller;

import com.example.shifter.dto.ShowSqueduleDTO;
import com.example.shifter.service.ShowScheduleService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ShowScheduleController {

    private final ShowScheduleService scheduleService;

    // ---------------------------------------------------------
    // MANAGER ENDPOINTS (FULL ACCESS)
    // ---------------------------------------------------------

    @GetMapping("/today")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ShowSqueduleDTO>> getToday() {
        return ResponseEntity.ok(scheduleService.getTodaySchedule());
    }

    @GetMapping("/date")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ShowSqueduleDTO>> getByDate(@RequestParam LocalDate date) {
        return ResponseEntity.ok(scheduleService.getScheduleForDate(date));
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ShowSqueduleDTO>> getRange(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return ResponseEntity.ok(scheduleService.getScheduleForRange(start, end));
    }

    @GetMapping("/week")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ShowSqueduleDTO>> getWeek(@RequestParam LocalDate weekStart) {
        return ResponseEntity.ok(scheduleService.getScheduleForWeek(weekStart));
    }

    @DeleteMapping("/{shiftId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> deleteShift(@PathVariable Long shiftId) {
        scheduleService.deleteShift(shiftId);
        return ResponseEntity.ok("Shift deleted successfully.");
    }

    // ---------------------------------------------------------
    // EMPLOYEE ENDPOINTS (SELF ONLY)
    // ---------------------------------------------------------

    @GetMapping("/me/today")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ShowSqueduleDTO>> getMyToday() {
        return ResponseEntity.ok(scheduleService.getScheduleForEmployeeOnDate(
                scheduleService.getCurrentUserId(),
                LocalDate.now()
        ));
    }

    @GetMapping("/me/date")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ShowSqueduleDTO>> getMyDate(@RequestParam LocalDate date) {
        return ResponseEntity.ok(scheduleService.getScheduleForEmployeeOnDate(
                scheduleService.getCurrentUserId(),
                date
        ));
    }

    @GetMapping("/me/week")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ShowSqueduleDTO>> getMyWeek(@RequestParam LocalDate weekStart) {
        return ResponseEntity.ok(scheduleService.getScheduleForEmployeeWeek(
                scheduleService.getCurrentUserId(),
                weekStart
        ));
    }

    @GetMapping("/me/range")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ShowSqueduleDTO>> getMyRange(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return ResponseEntity.ok(scheduleService.getScheduleForEmployeeRange(
                scheduleService.getCurrentUserId(),
                start,
                end
        ));
    }
}