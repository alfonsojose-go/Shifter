package com.example.shifter.controller.position_scheduling;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shifter.dto.availability.ApiResponse;
import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;
import com.example.shifter.service.position_scheduling.PositionSchedulingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/scheduling")
@RequiredArgsConstructor
@Validated
public class PositionSchedulingController {

    private final PositionSchedulingService positionSchedulingService;

    /**
     * GET /api/scheduling/scheduled_shift/positions
     * Returns all schedules with employee position data.
     */
    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> getAllSchedules() {
        log.info("GET /api/scheduling/positions - Getting all scheduled shifts with positions");

        try {
            List<EmployeeScheduleDTO> responses = positionSchedulingService.getAllSchedulesWithPositions();

            return ResponseEntity.ok(ApiResponse.success(
                    "Schedules retrieved successfully",
                    responses));

        } catch (Exception e) {
            log.error("Error getting all schedules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve schedules"));
        }
    }

    /**
     * GET /api/scheduling/scheduled_shift/positions/employee/{employeeId}
     * Returns schedules for a specific employee with their position data.
     */
    @GetMapping("/positions/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> getSchedulesByEmployee(
            @PathVariable Long employeeId) {
        log.info("GET /api/scheduling/scheduled_shift/positions/employee/{} - Getting schedules for employee", employeeId);

        try {
            List<EmployeeScheduleDTO> responses = positionSchedulingService.getSchedulesByEmployeeId(employeeId);

            return ResponseEntity.ok(ApiResponse.success(
                    "Employee schedules retrieved successfully",
                    responses));

        } catch (IllegalArgumentException e) {
            log.error("Employee not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error getting schedules for employee {}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve employee schedules"));
        }
    }
}