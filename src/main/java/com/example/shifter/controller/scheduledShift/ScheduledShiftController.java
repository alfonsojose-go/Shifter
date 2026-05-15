package com.example.shifter.controller.scheduledShift;

import com.example.shifter.dto.availability.ApiResponse;
import com.example.shifter.dto.scheduledShift.ScheduledShiftRequest;
import com.example.shifter.dto.scheduledShift.ScheduledShiftResponse;
import com.example.shifter.service.position_scheduling.ScheduledShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/scheduled-shifts") //Samara - fixed the api path
@RequiredArgsConstructor
@Validated
public class ScheduledShiftController {
private final ScheduledShiftService scheduledShiftService;

    /**
     * POST /api/scheduled-shifts
     * Creates a new scheduled shift
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledShiftResponse>> createScheduledShift(
            @Valid @RequestBody ScheduledShiftRequest request) {
        
        log.info("POST /api/scheduled-shifts - Creating scheduled shift for employee: {}", 
                request.getEmployeeId());

        try {
            ScheduledShiftResponse response = scheduledShiftService.createScheduledShift(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Scheduled shift created successfully",
                            response));

        } catch (IllegalArgumentException e) {
            log.error("Invalid input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error creating scheduled shift: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create scheduled shift"));
        }
    }
}
