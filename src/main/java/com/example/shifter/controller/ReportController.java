package com.example.shifter.controller;

import com.example.shifter.dto.ClockRecordReportDTO;
import com.example.shifter.dto.LaborCostDTO;
import com.example.shifter.dto.WeeklyReportDTO;
import com.example.shifter.service.AttendanceReportService;
import com.example.shifter.service.LaborCostService;
import com.example.shifter.util.CsvReportGenerator;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Centralized controller for all reporting endpoints:
 * - Attendance reports
 * - Labor cost reports
 * - Weekly summary (attendance + labor)
 * - CSV downloads
 *
 * Base path: /api/reports
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceReportService attendanceService;
    private final LaborCostService laborCostService;

    // ------------------------------------------------------------
    // 1. ATTENDANCE REPORTS
    // ------------------------------------------------------------

    /**
     * Returns weekly attendance for ALL employees.
     * Example:
     * GET /api/reports/attendance/week?weekStart=2026-02-23
     */
    @GetMapping("/attendance/week")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ClockRecordReportDTO>> getWeeklyAttendance(
            @RequestParam LocalDate weekStart) {

        return ResponseEntity.ok(attendanceService.getWeeklyReport(weekStart));
    }

    /**
     * Returns weekly attendance for ONE employee.
     * Example:
     * GET /api/reports/attendance/employee?employeeId=5&weekStart=2026-02-23
     */
    @GetMapping("/attendance/employee")
    @PreAuthorize("hasAnyRole('MANAGER','EMPLOYEE')")
    public ResponseEntity<List<ClockRecordReportDTO>> getEmployeeWeeklyAttendance(
            @RequestParam Long employeeId,
            @RequestParam LocalDate weekStart) {

        return ResponseEntity.ok(attendanceService.getWeeklyReportForEmployee(employeeId, weekStart));
    }

    /**
     * Downloads weekly attendance as CSV.
     * Example:
     * GET /api/reports/attendance/week/csv?weekStart=2026-02-23
     */
    @GetMapping("/attendance/week/csv")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<byte[]> downloadWeeklyAttendanceCsv(
            @RequestParam LocalDate weekStart) {

        List<ClockRecordReportDTO> report = attendanceService.getWeeklyReport(weekStart);
        String csv = CsvReportGenerator.generateAttendanceCsv(report);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=attendance_report.csv")
                .header("Content-Type", "text/csv")
                .body(csv.getBytes());
    }

    // ------------------------------------------------------------
    // 2. LABOR COST REPORTS
    // ------------------------------------------------------------

    /**
     * Returns weekly labor cost summary.
     * Example:
     * GET /api/reports/labor-cost?weekStart=2026-02-23
     */
    @GetMapping("/labor-cost")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LaborCostDTO> getWeeklyLaborCost(
            @RequestParam LocalDate weekStart) {

        return ResponseEntity.ok(laborCostService.getWeeklyLaborCost(weekStart));
    }

    // ------------------------------------------------------------
    // 3. WEEKLY SUMMARY (ATTENDANCE + LABOR COST)
    // ------------------------------------------------------------

    /**
     * Returns a combined weekly summary:
     * - Attendance report
     * - Labor cost report
     * - Total scheduled cost
     * - Total actual cost
     *
     * Example:
     * GET /api/reports/weekly-summary?weekStart=2026-02-23
     */
    @GetMapping("/weekly-summary")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WeeklyReportDTO> getWeeklySummary(
            @RequestParam LocalDate weekStart) {

        var attendance = attendanceService.getWeeklyReport(weekStart);
        var labor = laborCostService.getWeeklyLaborCost(weekStart);

        double totalActual = attendance.stream()
                .mapToDouble(a -> a.getActualCost() != null ? a.getActualCost() : 0.0)
                .sum();

        WeeklyReportDTO dto = new WeeklyReportDTO(
                attendance,
                labor,
                labor.getTotalScheduled(),
                totalActual
        );

        return ResponseEntity.ok(dto);
    }
}