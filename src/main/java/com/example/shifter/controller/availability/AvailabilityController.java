package com.example.shifter.controller.availability;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.example.shifter.dto.availability.AvailabilityResponse;
import com.example.shifter.dto.availability.CreateAvailabilityRequest;
import com.example.shifter.dto.availability.CreateAvailabilityRequest.AvailabilitySlot;
import com.example.shifter.dto.availability.UpdateAvailabilityRequest;
import com.example.shifter.dto.availability.EmployeeResponse;
import com.example.shifter.model.availability.Availability;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

import com.example.shifter.service.availability.AvailabilityService;

@RestController
@RequestMapping("/api/employee/availabilities")
@Validated
public class AvailabilityController {
    private static final Logger log = LoggerFactory.getLogger(AvailabilityController.class);

    private final AvailabilityService availabilityService;

    @Autowired
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createAvailability(
            @Valid @RequestBody CreateAvailabilityRequest request) {
        log.info("POST /api/availabilities - Creating batch availability for employee: {}", request.getEmployeeId());

        try {
            // Handle null or empty list
            List<AvailabilitySlot> slots;
            if (request.getAvailabilities() == null) {
                slots = Collections.emptyList();
            } else {
                slots = request.getAvailabilities().stream()
                        .map(reqSlot -> new AvailabilitySlot(
                                reqSlot.getDayOfWeek(),
                                reqSlot.getStartTime(),
                                reqSlot.getEndTime()))
                        .collect(Collectors.toList());
            }

            availabilityService.createAvailability(request.getEmployeeId(), slots);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            String.format("Successfully created %d availability slots", slots.size())));

        } catch (IllegalArgumentException e) {
            log.error("Error creating availability: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * Get all availabilities
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAllAvailabilities() {
        log.info("GET /api/availabilities - Getting all availabilities");

        try {
            List<Availability> availabilities = availabilityService.getAllAvailabilities();
            List<AvailabilityResponse> responses = availabilities.stream()
                    .map(AvailabilityResponse::fromAvailability)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    "Availabilities retrieved successfully",
                    responses));

        } catch (Exception e) {
            log.error("Error getting all availabilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve availabilities"));
        }
    }

    /**
     * Get availability by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailabilityById(@PathVariable Long id) {
        log.info("GET /api/availabilities/{} - Getting availability by ID", id);

        try {
            Availability availability = availabilityService.getAvailabilityById(id);
            AvailabilityResponse response = AvailabilityResponse.fromAvailability(availability);

            return ResponseEntity.ok(ApiResponse.success(
                    "Availability retrieved successfully",
                    response));

        } catch (IllegalArgumentException e) {
            log.error("Availability not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve availability"));
        }
    }

    /**
     * Get availabilities by employee ID
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAvailabilitiesByEmployee(
            @PathVariable Long employeeId) {
        log.info("GET /api/availabilities/employee/{} - Getting availabilities by employee", employeeId);

        try {
            List<Availability> availabilities = availabilityService.getAvailabilitiesByEmployee(employeeId);
            List<AvailabilityResponse> responses = availabilities.stream()
                    .map(AvailabilityResponse::fromAvailability)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    "Employee availabilities retrieved successfully",
                    responses));

        } catch (IllegalArgumentException e) {
            log.error("Error getting employee availabilities: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting employee availabilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve employee availabilities"));
        }
    }

    /**
     * Get availabilities by employee ID and day
     */
    @GetMapping("/employee/{employeeId}/day/{day}")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAvailabilitiesByEmployeeAndDay(
            @PathVariable Long employeeId,
            @PathVariable Availability.DayOfWeek day) {
        log.info("GET /api/availabilities/employee/{}/day/{} - Getting availabilities by employee and day",
                employeeId, day);

        try {
            List<Availability> availabilities = availabilityService.getAvailabilitiesByEmployeeAndDay(employeeId, day);
            List<AvailabilityResponse> responses = availabilities.stream()
                    .map(AvailabilityResponse::fromAvailability)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                    "Employee day availabilities retrieved successfully",
                    responses));

        } catch (IllegalArgumentException e) {
            log.error("Error getting employee day availabilities: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting employee day availabilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve employee day availabilities"));
        }
    }

    /**
     * Update availability
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        log.info("PUT /api/availabilities/{} - Updating availability", id);

        try {
            availabilityService.updateAvailabilityById(
                    id,
                    request.getDayOfWeek(),
                    request.getStartTime(),
                    request.getEndTime());

            return ResponseEntity.ok(ApiResponse.success("Availability updated successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Error updating availability: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update availability"));
        }
    }

    /**
     * Delete availability
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAvailability(@PathVariable Long id) {
        log.info("DELETE /api/availabilities/{} - Deleting availability", id);

        try {
            availabilityService.deleteAvailabilityById(id);
            return ResponseEntity.ok(ApiResponse.success("Availability deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Error deleting availability: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete availability"));
        }
    }

    /**
     * Delete all availabilities for an employee
     */
    @DeleteMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<String>> deleteAllAvailabilitiesOfEmployee(
            @PathVariable Long employeeId) {
        log.info("DELETE /api/availabilities/employee/{} - Deleting all availabilities for employee", employeeId);

        try {
            availabilityService.deleteAllAvailabilitiesOfEmployee(employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "All availabilities for employee deleted successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Error deleting employee availabilities: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting employee availabilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete employee availabilities"));
        }
    }

    /**
     * Validate time range
     */
    @PostMapping("/validate-time")
    public ResponseEntity<ApiResponse<String>> validateTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        log.info("POST /api/availabilities/validate-time - Validating time range: {} to {}", startTime, endTime);

        try {
            availabilityService.validateTimeRange(startTime, endTime);
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
            @PathVariable Availability.DayOfWeek day) {
        log.info("GET /api/availabilities/day/{}/employees - Getting available employees", day);

        try {
            List<com.example.shifter.model.User> employees = availabilityService.getAvailableEmployeesByDay(day);

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
     * Check if employee has any availabilities
     */
    @GetMapping("/employee/{employeeId}/has-availabilities")
    public ResponseEntity<ApiResponse<Boolean>> hasAvailabilities(@PathVariable Long employeeId) {
        log.info("GET /api/availabilities/employee/{}/has-availabilities - Checking if employee has availabilities",
                employeeId);

        try {
            boolean hasAvailabilities = availabilityService.hasAvailabilities(employeeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Availability check completed successfully",
                    hasAvailabilities));

        } catch (Exception e) {
            log.error("Error checking employee availabilities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check employee availabilities"));
        }
    }
}