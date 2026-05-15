package com.example.shifter.service.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;
import com.example.shifter.model.Position;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.repository.position_scheduling.PositionSchedulingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor  // This creates constructor for final fields
public class PositionSchedulingServiceImpl implements PositionSchedulingService {

    private final PositionSchedulingRepository positionSchedulingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeScheduleDTO> getAllSchedulesWithPositions() {
        log.info("Fetching all schedules with employee position data");
        return positionSchedulingRepository.findAllWithEmployeeAndPosition()
                .stream()
                .map(this::toDTO)  // Can use method reference now
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeScheduleDTO> getSchedulesByEmployeeId(Long employeeId) {
        log.info("Fetching schedules for employee id={}", employeeId);

        // Repository already returns DTOs
        Collection<EmployeeScheduleDTO> schedules = positionSchedulingRepository
                .findByEmployeeIdWithPosition(employeeId);

        // Convert Collection to List if needed
        return new ArrayList<>(schedules);
    }

    // --- Mapping helper ---
    private EmployeeScheduleDTO toDTO(Scheduling schedule) {
        User employee = schedule.getEmployee();
        
        // Add null check for employee
        if (employee == null) {
            log.warn("Scheduling ID {} has no employee associated", schedule.getSchedulingId());
            return new EmployeeScheduleDTO(
                    null,
                    "No Employee",
                    "Unassigned",
                    BigDecimal.ZERO,
                    schedule.getSchedulingId(),
                    schedule.getDayOfWeek(),
                    schedule.getStartTime(),
                    schedule.getEndTime()
            );
        }

        // Position is nullable — handle gracefully
        Position position = employee.getPosition();
        String positionName = position != null ? position.getName() : "Unassigned";
        BigDecimal hourlyWage = position != null ? position.getHourlyWage() : BigDecimal.ZERO;

        return new EmployeeScheduleDTO(
                employee.getId(),
                employee.getFullName(),
                positionName,
                hourlyWage,
                schedule.getSchedulingId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}