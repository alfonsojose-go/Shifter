package com.example.shifter.controller.scheduling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shifter.dto.availability.ApiResponse;
import com.example.shifter.dto.scheduling.SchedulingResponse;
import com.example.shifter.dto.scheduling.CreateSchedulingRequest;
import com.example.shifter.dto.scheduling.CreateSchedulingRequest.SchedulingSlot;
import com.example.shifter.dto.scheduling.UpdateSchedulingRequest;
import com.example.shifter.dto.availability.EmployeeResponse;
import com.example.shifter.dto.scheduling.BatchSchedulingRequest;
import com.example.shifter.model.scheduling.Scheduling;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

import com.example.shifter.service.scheduling.SchedulingService;

@RestController
@RequestMapping("/api/scheduling")
@Validated
public class SchedulingController {
    private static final Logger log = LoggerFactory.getLogger(SchedulingController.class);

    private final SchedulingService schedulingService;

    @Autowired
    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }


    /**
     * One schedule at a time
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createSchedule(
            @Valid @RequestBody CreateSchedulingRequest request) {
        log.info("POST /api/scheduling - Creating batch scheduling for employee: {}", request.getEmployeeId());

        try {
            // Handle null or empty list
            List<SchedulingSlot> slots;
            if (request.getSchedules() == null) {
                slots = Collections.emptyList();
            } else {
                slots = request.getSchedules().stream()
                        .map(reqSlot -> new SchedulingSlot(
                                reqSlot.getDayOfWeek(),
                                reqSlot.getStartTime(),
                                reqSlot.getEndTime()))
                        .collect(Collectors.toList());
            }

            schedulingService.createSchedule(request.getEmployeeId(), slots);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            String.format("Successfully created %d scheduling slots", slots.size())));

        } catch (IllegalArgumentException e) {
            log.error("Error creating scheduling: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating scheduling: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<String>> createBatchSchedules(
            @Valid @RequestBody BatchSchedulingRequest request) {
        log.info("POST /api/scheduling/batch - Creating schedules for {} employees", 
                request.getEmployees().size());

        try {
            int totalSlots = 0;
            for (BatchSchedulingRequest.EmployeeScheduleRequest empReq : request.getEmployees()) {
                List<SchedulingSlot> slots = empReq.getSchedules() == null 
                    ? Collections.emptyList() 
                    : empReq.getSchedules();
                
                schedulingService.createSchedule(empReq.getEmployeeId(), slots);
                totalSlots += slots.size();
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            String.format("Successfully created schedules for %d employees (%d total slots)", 
                                    request.getEmployees().size(), totalSlots)));

        } catch (IllegalArgumentException e) {
            log.error("Error creating batch scheduling: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating batch scheduling: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * Get all schedules
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SchedulingResponse>>> getAllSchedules() {
        log.info("GET /api/scheduling - Getting all schedules");

        try {
            List<Scheduling> schedules = schedulingService.getAllSchedules();
            List<SchedulingResponse> responses = schedules.stream()
                    .map(SchedulingResponse::fromScheduling)
                    .toList();

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
     * Get scheduling by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchedulingResponse>> getScheduleById(@PathVariable Long id) {
        log.info("GET /api/scheduling/{id} - Getting scheduling by ID", id);

        try {
            Scheduling scheduling = schedulingService.getScheduleById(id);
            SchedulingResponse response = SchedulingResponse.fromScheduling(scheduling);

            return ResponseEntity.ok(ApiResponse.success(
                    "Scheduling retrieved successfully",
                    response));

        } catch (IllegalArgumentException e) {
            log.error("Scheduling not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting scheduling: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve scheduling"));
        }
    }

    /**
     * Get schedules by employee ID
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<SchedulingResponse>>> getSchedulesByEmployee(
            @PathVariable Long employeeId) {
        log.info("GET /api/scheduling/employee/{employeeId} - Getting schedules by employee", employeeId);

        try {
            List<Scheduling> schedules = schedulingService.getSchedulesByEmployee(employeeId);
            List<SchedulingResponse> responses = schedules.stream()
                    .map(SchedulingResponse::fromScheduling)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    "Employee schedules retrieved successfully",
                    responses));

        } catch (IllegalArgumentException e) {
            log.error("Error getting employee schedules: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting employee schedules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve employee schedules"));
        }
    }

    /**
     * Get schedules by employee ID and day
     */
    @GetMapping("/employee/{employeeId}/day/{day}")
    public ResponseEntity<ApiResponse<List<SchedulingResponse>>> getSchedulesByEmployeeAndDay(
            @PathVariable Long employeeId,
            @PathVariable Scheduling.DayOfWeek day) {
        log.info("GET /api/scheduling/employee/{}/day/{} - Getting schedules by employee and day",
                employeeId, day);

        try {
            List<Scheduling> schedules = schedulingService.getSchedulesByEmployeeAndDay(employeeId, day);
            List<SchedulingResponse> responses = schedules.stream()
                    .map(SchedulingResponse::fromScheduling)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    "Employee day schedules retrieved successfully",
                    responses));

        } catch (IllegalArgumentException e) {
            log.error("Error getting employee day schedules: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting employee day schedules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve employee day schedules"));
        }
    }

    /**
     * Update scheduling
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateScheduling(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSchedulingRequest request) {
        log.info("PUT /api/scheduling/{id} - Updating scheduling", id);

        try {
            schedulingService.updateScheduleById(
                    id,
                    request.getDayOfWeek(),
                    request.getStartTime(),
                    request.getEndTime());

            return ResponseEntity.ok(ApiResponse.success("Scheduling updated successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Error updating scheduling: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating scheduling: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update scheduling"));
        }
    }

    /**
     * Delete scheduling
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteScheduling(@PathVariable Long id) {
        log.info("DELETE /api/scheduling/{} - Deleting scheduling", id);

        try {
            schedulingService.deleteScheduleById(id);
            return ResponseEntity.ok(ApiResponse.success("Scheduling deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Error deleting scheduling: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting scheduling: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete scheduling"));
        }
    }

    /**
     * Delete all schedule for an employee
     */
    @DeleteMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<String>> deleteAllScheduleOfEmployee(
            @PathVariable Long employeeId) {
        log.info("DELETE /api/scheduling/employee/{employeeId} - Deleting all schedules for employee", employeeId);

        try {
            schedulingService.deleteAllScheduleOfEmployee(employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "All schedules for employee deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Error deleting employee schedules: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting employee schedules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete employee schedules"));
        }
    }

    /**
     * Validate time range
     */
    @PostMapping("/validate-time")
    public ResponseEntity<ApiResponse<String>> validateTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        log.info("POST /api/scheduling/validate-time - Validating time range: {} to {}", startTime, endTime);

        try {
            schedulingService.validateTimeRange(startTime, endTime);
            return ResponseEntity.ok(ApiResponse.success("Time range is valid"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error validating time range: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to validate time range"));
        }
    }

    /**
     * Get available employees by day
     */
    @GetMapping("/day/{day}/employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAvailableEmployeesByDay(
            @PathVariable Scheduling.DayOfWeek day) {
        log.info("GET /api/scheduling/day/{}/employees - Getting available employees", day);

        try {
            List<com.example.shifter.model.User> employees = schedulingService.getAvailableEmployeesByDay(day);

            List<EmployeeResponse> employeeResponses = employees.stream()
                    .map(user -> {
                        EmployeeResponse response = new EmployeeResponse();
                        response.setId(user.getId());
                        response.setFullName(user.getFullName());
                        response.setUsername(user.getUsername());
                        response.setEmail(user.getEmail());
                        return response;
                    })
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    "Available employees retrieved successfully",
                    employeeResponses));

        } catch (Exception e) {
            log.error("Error getting available employees: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve available employees"));
        }
    }

    /**
     * Check if employee has any schedule
     */
    @GetMapping("/employee/{employeeId}/has-schedule")
    public ResponseEntity<ApiResponse<Boolean>> hasSchedule(@PathVariable Long employeeId) {
        log.info("GET /api/scheduling/employee/{}/has-schedule - Checking if employee has schedule",
                employeeId);

        try {
            boolean hasSchedule = schedulingService.hasSchedule(employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Scheduling check completed successfully",
                    hasSchedule));

        } catch (Exception e) {
            log.error("Error checking employee schedules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check employee schedule"));
        }
    }
}