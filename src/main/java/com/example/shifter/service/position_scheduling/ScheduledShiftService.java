package com.example.shifter.service.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeShiftDTO;
import com.example.shifter.dto.scheduledShift.ScheduledShiftRequest;
import com.example.shifter.dto.scheduledShift.ScheduledShiftResponse;

import java.util.List;

public interface ScheduledShiftService {

    /**
     * Returns all shifts enriched with each employee's position data.
     * Intended for MANAGER role.
     */
    List<EmployeeShiftDTO> getAllShiftsWithPositions();

    /**
     * Returns all shifts for a specific employee.
     * Intended for EMPLOYEE role (own data).
     */
    List<EmployeeShiftDTO> getShiftsByEmployeeId(Long employeeId);

    ScheduledShiftResponse createScheduledShift(ScheduledShiftRequest request);
}