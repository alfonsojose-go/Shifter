package com.example.shifter.service.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeShiftDTO;
import com.example.shifter.model.Position;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.model.User;
import com.example.shifter.repository.ScheduledShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledShiftServiceImplTest {

    @Mock
    private ScheduledShiftRepository scheduledShiftRepository;

    @InjectMocks
    private ScheduledShiftServiceImpl scheduledShiftService;

    private User testEmployee;
    private User testEmployeeNoPosition;
    private Position testPosition;
    private ScheduledShift testShift;
    private ScheduledShift testShiftNoPosition;

    @BeforeEach
    void setUp() {
        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setName("Cashier");
        testPosition.setHourlyWage(new BigDecimal("18.50"));

        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setFullName("John Doe");
        testEmployee.setUsername("johndoe");
        testEmployee.setEmail("john@example.com");
        testEmployee.setPosition(testPosition);

        // Employee with no position assigned
        testEmployeeNoPosition = new User();
        testEmployeeNoPosition.setId(2L);
        testEmployeeNoPosition.setFullName("Jane Smith");
        testEmployeeNoPosition.setUsername("janesmith");
        testEmployeeNoPosition.setEmail("jane@example.com");
        testEmployeeNoPosition.setPosition(null);

        // 2025-06-02 is a Monday — dayOfWeek is set by @PrePersist on the entity,
        // so we set it manually here to simulate what the DB would return
        testShift = new ScheduledShift();
        testShift.setId(1L);
        testShift.setEmployee(testEmployee);
        testShift.setDate(LocalDate.of(2025, 6, 2));
        testShift.setDayOfWeek(DayOfWeek.MONDAY);
        testShift.setStartTime(LocalTime.of(9, 0));
        testShift.setEndTime(LocalTime.of(17, 0));

        // 2025-06-03 is a Tuesday
        testShiftNoPosition = new ScheduledShift();
        testShiftNoPosition.setId(2L);
        testShiftNoPosition.setEmployee(testEmployeeNoPosition);
        testShiftNoPosition.setDate(LocalDate.of(2025, 6, 3));
        testShiftNoPosition.setDayOfWeek(DayOfWeek.TUESDAY);
        testShiftNoPosition.setStartTime(LocalTime.of(8, 0));
        testShiftNoPosition.setEndTime(LocalTime.of(16, 0));
    }

    // ========== GET ALL SHIFTS WITH POSITIONS TESTS ==========

    @Test
    void getAllShiftsWithPositions_ReturnsMappedDTOs() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShift));

        // Act
        List<EmployeeShiftDTO> result = scheduledShiftService.getAllShiftsWithPositions();

        // Assert
        assertEquals(1, result.size());
        verify(scheduledShiftRepository).findAllWithEmployeeAndPosition();
    }

    @Test
    void getAllShiftsWithPositions_MapsEmployeeFieldsCorrectly() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShift));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getAllShiftsWithPositions().get(0);

        // Assert
        assertEquals(1L, dto.getEmployeeId());
        assertEquals("John Doe", dto.getFullName());
    }

    @Test
    void getAllShiftsWithPositions_MapsPositionFieldsCorrectly() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShift));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getAllShiftsWithPositions().get(0);

        // Assert
        assertEquals("Cashier", dto.getPositionName());
        assertEquals(new BigDecimal("18.50"), dto.getHourlyWage());
    }

    @Test
    void getAllShiftsWithPositions_MapsShiftFieldsCorrectly() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShift));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getAllShiftsWithPositions().get(0);

        // Assert
        assertEquals(1L, dto.getShiftId());
        assertEquals(LocalDate.of(2025, 6, 2), dto.getDate());
        assertEquals(DayOfWeek.MONDAY, dto.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), dto.getStartTime());
        assertEquals(LocalTime.of(17, 0), dto.getEndTime());
    }

    @Test
    void getAllShiftsWithPositions_DayOfWeekMatchesDate() {
        // Arrange — validates the @PrePersist contract: 2025-06-02 must be MONDAY
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShift));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getAllShiftsWithPositions().get(0);

        // Assert
        assertEquals(dto.getDate().getDayOfWeek(), dto.getDayOfWeek());
    }

    @Test
    void getAllShiftsWithPositions_NullPosition_DefaultsToUnassignedAndZeroWage() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShiftNoPosition));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getAllShiftsWithPositions().get(0);

        // Assert — service must not throw and must fall back gracefully
        assertEquals("Unassigned", dto.getPositionName());
        assertEquals(BigDecimal.ZERO, dto.getHourlyWage());
    }

    @Test
    void getAllShiftsWithPositions_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(Collections.emptyList());

        // Act
        List<EmployeeShiftDTO> result = scheduledShiftService.getAllShiftsWithPositions();

        // Assert
        assertTrue(result.isEmpty());
        verify(scheduledShiftRepository).findAllWithEmployeeAndPosition();
    }

    @Test
    void getAllShiftsWithPositions_MultipleShifts_AllMapped() {
        // Arrange
        when(scheduledShiftRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testShift, testShiftNoPosition));

        // Act
        List<EmployeeShiftDTO> result = scheduledShiftService.getAllShiftsWithPositions();

        // Assert
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
        assertEquals("Jane Smith", result.get(1).getFullName());
        assertEquals("Cashier", result.get(0).getPositionName());
        assertEquals("Unassigned", result.get(1).getPositionName());
    }

    // ========== GET SHIFTS BY EMPLOYEE ID TESTS ==========

    @Test
    void getShiftsByEmployeeId_ReturnsMappedDTOs() {
        // Arrange
        when(scheduledShiftRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testShift));

        // Act
        List<EmployeeShiftDTO> result = scheduledShiftService.getShiftsByEmployeeId(1L);

        // Assert
        assertEquals(1, result.size());
        verify(scheduledShiftRepository).findByEmployeeIdWithPosition(1L);
    }

    @Test
    void getShiftsByEmployeeId_MapsAllFieldsCorrectly() {
        // Arrange
        when(scheduledShiftRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testShift));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getShiftsByEmployeeId(1L).get(0);

        // Assert
        assertEquals(1L, dto.getEmployeeId());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("Cashier", dto.getPositionName());
        assertEquals(new BigDecimal("18.50"), dto.getHourlyWage());
        assertEquals(1L, dto.getShiftId());
        assertEquals(LocalDate.of(2025, 6, 2), dto.getDate());
        assertEquals(DayOfWeek.MONDAY, dto.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), dto.getStartTime());
        assertEquals(LocalTime.of(17, 0), dto.getEndTime());
    }

    @Test
    void getShiftsByEmployeeId_NullPosition_DefaultsToUnassignedAndZeroWage() {
        // Arrange
        when(scheduledShiftRepository.findByEmployeeIdWithPosition(2L))
                .thenReturn(List.of(testShiftNoPosition));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getShiftsByEmployeeId(2L).get(0);

        // Assert
        assertEquals("Unassigned", dto.getPositionName());
        assertEquals(BigDecimal.ZERO, dto.getHourlyWage());
    }

    @Test
    void getShiftsByEmployeeId_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(scheduledShiftRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(Collections.emptyList());

        // Act
        List<EmployeeShiftDTO> result = scheduledShiftService.getShiftsByEmployeeId(1L);

        // Assert
        assertTrue(result.isEmpty());
        verify(scheduledShiftRepository).findByEmployeeIdWithPosition(1L);
    }

    @Test
    void getShiftsByEmployeeId_MultipleShifts_AllMapped() {
        // Arrange
        ScheduledShift thursdayShift = new ScheduledShift();
        thursdayShift.setId(3L);
        thursdayShift.setEmployee(testEmployee);
        thursdayShift.setDate(LocalDate.of(2025, 6, 5));    // Thursday
        thursdayShift.setDayOfWeek(DayOfWeek.THURSDAY);
        thursdayShift.setStartTime(LocalTime.of(10, 0));
        thursdayShift.setEndTime(LocalTime.of(18, 0));

        when(scheduledShiftRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testShift, thursdayShift));

        // Act
        List<EmployeeShiftDTO> result = scheduledShiftService.getShiftsByEmployeeId(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(DayOfWeek.MONDAY, result.get(0).getDayOfWeek());
        assertEquals(DayOfWeek.THURSDAY, result.get(1).getDayOfWeek());
        assertEquals(LocalDate.of(2025, 6, 2), result.get(0).getDate());
        assertEquals(LocalDate.of(2025, 6, 5), result.get(1).getDate());
    }

    @Test
    void getShiftsByEmployeeId_DayOfWeekMatchesDate() {
        // Arrange
        when(scheduledShiftRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testShift));

        // Act
        EmployeeShiftDTO dto = scheduledShiftService.getShiftsByEmployeeId(1L).get(0);

        // Assert — the date 2025-06-02 must always resolve to MONDAY
        assertEquals(dto.getDate().getDayOfWeek(), dto.getDayOfWeek());
    }
}