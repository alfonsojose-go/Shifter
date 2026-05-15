package com.example.shifter.controller;

import com.example.shifter.dto.EmployeeRecentRequestDTO;
import com.example.shifter.dto.ManagerOldestRequestDTO;
import com.example.shifter.service.DashboardService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * This Controller has the apis to employee and manager dashboard
 */

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ---------------- EMPLOYEE ----------------

    @GetMapping("/employee/pending/count")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getEmployeePendingCount() {
        return ResponseEntity.ok(dashboardService.getEmployeePendingCount());
    }

    @GetMapping("/employee/recent")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<EmployeeRecentRequestDTO>> getEmployeeRecentRequests() {
        return ResponseEntity.ok(dashboardService.getEmployeeRecentRequests());
    }

    // ---------------- MANAGER ----------------

    @GetMapping("/manager/active-employees")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getActiveEmployeesCount() {
        return ResponseEntity.ok(dashboardService.getActiveEmployeesCount());
    }

    @GetMapping("/manager/pending/count")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getPendingRequestsCount() {
        return ResponseEntity.ok(dashboardService.getPendingRequestsCount());
    }

    @GetMapping("/manager/pending/oldest")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ManagerOldestRequestDTO>> getOldestPendingRequests() {
        return ResponseEntity.ok(dashboardService.getOldestPendingRequests());
    }
}