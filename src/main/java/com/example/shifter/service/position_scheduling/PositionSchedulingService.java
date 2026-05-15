package com.example.shifter.service.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;

import java.util.List;

public interface PositionSchedulingService {

    /**
     * Returns all schedules enriched with each employee's position data.
     * Used by managers to view the full schedule.
     */
    List<EmployeeScheduleDTO> getAllSchedulesWithPositions();

    /**
     * Returns all schedules for a specific employee.
     * Used by employees to view their own schedule.
     */
    List<EmployeeScheduleDTO> getSchedulesByEmployeeId(Long employeeId);

}