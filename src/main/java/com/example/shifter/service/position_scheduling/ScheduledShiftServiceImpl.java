package com.example.shifter.service.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeShiftDTO;
import com.example.shifter.model.Position;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.model.User;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.ScheduledShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shifter.dto.scheduledShift.ScheduledShiftRequest;
import com.example.shifter.dto.scheduledShift.ScheduledShiftResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledShiftServiceImpl implements ScheduledShiftService {

    private final ScheduledShiftRepository scheduledShiftRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeShiftDTO> getAllShiftsWithPositions() {
        log.info("Fetching all scheduled shifts with employee position data");
        return scheduledShiftRepository.findAllWithEmployeeAndPosition()
                .stream()
                .map(shift -> toDTO(shift))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeShiftDTO> getShiftsByEmployeeId(Long employeeId) {
        log.info("Fetching scheduled shifts for employee id={}", employeeId);
        return scheduledShiftRepository.findByEmployeeIdWithPosition(employeeId)
                .stream()
                .map(shift -> toDTO(shift))
                .collect(Collectors.toList());
    }

    // --- Mapping helper ---

    private EmployeeShiftDTO toDTO(ScheduledShift shift) {
        User employee = shift.getEmployee();

        // Position is nullable — handle gracefully
        Position position = employee.getPosition();
        String positionName = position != null ? position.getName() : "Unassigned";
        BigDecimal hourlyWage = position != null ? position.getHourlyWage() : BigDecimal.ZERO;

        return new EmployeeShiftDTO(
                employee.getId(),
                employee.getFullName(),
                positionName,
                hourlyWage,
                shift.getId(),
                shift.getDate(),
                shift.getDayOfWeek(),   // already set by @PrePersist/@PreUpdate on the entity
                shift.getStartTime(),
                shift.getEndTime()
        );
    }


     // --- Posting in ScheduledShift entity ---

    @Override
    @Transactional
    public ScheduledShiftResponse createScheduledShift(ScheduledShiftRequest request) {
        log.info("Creating scheduled shift for employee ID: {}", request.getEmployeeId());

        // Find the employee
        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Employee not found with ID: " + request.getEmployeeId()));

        //Samara - fix to the shift overlap on Alfonso's code
        // Check for overlapping shifts
        // Prevent overlapping shifts for the same employee on the same date
        List<ScheduledShift> overlaps = scheduledShiftRepository.findOverlappingShifts(
                request.getEmployeeId(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!overlaps.isEmpty()) {
            ScheduledShift conflict = overlaps.get(0);
            throw new IllegalArgumentException(
                    String.format(
                            "Shift overlaps with existing shift on %s from %s to %s",
                            conflict.getDate(),
                            conflict.getStartTime(),
                            conflict.getEndTime()
                    )
            );
        }

        // Create and save the shift
        ScheduledShift shift = new ScheduledShift(
                employee,
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        ScheduledShift savedShift = scheduledShiftRepository.save(shift);
        log.info("Created scheduled shift with ID: {}", savedShift.getId());

        // Map to response DTO
        return mapToResponse(savedShift);
    }

    private ScheduledShiftResponse mapToResponse(ScheduledShift shift) {
        User employee = shift.getEmployee();
        String positionName = employee.getPosition() != null ? 
            employee.getPosition().getName() : "Unassigned";
        BigDecimal hourlyWage = employee.getPosition() != null ? 
            employee.getPosition().getHourlyWage() : BigDecimal.ZERO;

        return new ScheduledShiftResponse(
                shift.getId(),
                employee.getId(),
                employee.getFullName(),
                positionName,
                hourlyWage,
                shift.getDate(),
                shift.getDayOfWeek(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getCreatedAt()
        );
    }
}