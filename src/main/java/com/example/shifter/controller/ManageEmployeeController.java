package com.example.shifter.controller;

import com.example.shifter.dto.EmployeeTableResponse;
import com.example.shifter.dto.UpdateEmployeeTableRequest;
import com.example.shifter.service.ManageEmployeeTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emp/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_MANAGER')")
public class ManageEmployeeController {
    private final ManageEmployeeTableService service;

    /**
     * Dashboard table data.
     */
    @GetMapping
    public List<EmployeeTableResponse> getAllEmployees() {
        return service.getAllEmployees();
    }

    /**
     * Search employee by name.
     */
    @GetMapping("/search")
    public EmployeeTableResponse searchEmployee(@RequestParam String name) {
        return service.getEmployeeByName(name);
    }

    /**
     * Update employee job info.
     */
    @PutMapping("/{id}")
    public void updateEmployee(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeTableRequest request
    ) {
        service.updateEmployee(id, request);
    }
}
