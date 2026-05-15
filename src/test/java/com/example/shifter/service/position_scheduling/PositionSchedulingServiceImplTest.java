package com.example.shifter.service.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;
import com.example.shifter.model.Position;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.repository.position_scheduling.PositionSchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionSchedulingServiceImplTest {

    @Mock
    private PositionSchedulingRepository positionSchedulingRepository;

    @InjectMocks
    private PositionSchedulingServiceImpl positionSchedulingService;

    private User testEmployee;
    private User testEmployeeNoPosition;
    private Position testPosition;
    private Scheduling testScheduling;
    private Scheduling testSchedulingNoPosition;
    private EmployeeScheduleDTO testScheduleDTO;
    private EmployeeScheduleDTO testScheduleNoPositionDTO;

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

        testScheduling = new Scheduling();
        testScheduling.setSchedulingId(1L);
        testScheduling.setEmployee(testEmployee);
        testScheduling.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        testScheduling.setStartTime(LocalTime.of(9, 0));
        testScheduling.setEndTime(LocalTime.of(17, 0));

        testSchedulingNoPosition = new Scheduling();
        testSchedulingNoPosition.setSchedulingId(2L);
        testSchedulingNoPosition.setEmployee(testEmployeeNoPosition);
        testSchedulingNoPosition.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        testSchedulingNoPosition.setStartTime(LocalTime.of(8, 0));
        testSchedulingNoPosition.setEndTime(LocalTime.of(16, 0));

        // Create expected DTOs for the repository to return
        testScheduleDTO = new EmployeeScheduleDTO(
                1L,
                "John Doe",
                "Cashier",
                new BigDecimal("18.50"),
                1L,
                Scheduling.DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        testScheduleNoPositionDTO = new EmployeeScheduleDTO(
                2L,
                "Jane Smith",
                "Unassigned",
                BigDecimal.ZERO,
                2L,
                Scheduling.DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0)
        );
    }

    // ========== GET ALL SCHEDULES WITH POSITIONS TESTS ==========

    @Test
    void getAllSchedulesWithPositions_ReturnsMappedDTOs() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testScheduling));

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getAllSchedulesWithPositions();

        // Assert
        assertEquals(1, result.size());
        verify(positionSchedulingRepository, times(1)).findAllWithEmployeeAndPosition();
    }

    @Test
    void getAllSchedulesWithPositions_MapsEmployeeFieldsCorrectly() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testScheduling));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getAllSchedulesWithPositions().get(0);

        // Assert
        assertEquals(1L, dto.getEmployeeId());
        assertEquals("John Doe", dto.getFullName());
    }

    @Test
    void getAllSchedulesWithPositions_MapsPositionFieldsCorrectly() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testScheduling));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getAllSchedulesWithPositions().get(0);

        // Assert
        assertEquals("Cashier", dto.getPositionName());
        assertEquals(new BigDecimal("18.50"), dto.getHourlyWage());
    }

    @Test
    void getAllSchedulesWithPositions_MapsScheduleFieldsCorrectly() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testScheduling));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getAllSchedulesWithPositions().get(0);

        // Assert
        assertEquals(1L, dto.getScheduleId());
        assertEquals(Scheduling.DayOfWeek.MONDAY, dto.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), dto.getStartTime());
        assertEquals(LocalTime.of(17, 0), dto.getEndTime());
    }

    @Test
    void getAllSchedulesWithPositions_NullPosition_DefaultsToUnassignedAndZeroWage() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testSchedulingNoPosition));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getAllSchedulesWithPositions().get(0);

        // Assert — service must not throw and must fall back gracefully
        assertEquals("Unassigned", dto.getPositionName());
        assertEquals(BigDecimal.ZERO, dto.getHourlyWage());
    }

    @Test
    void getAllSchedulesWithPositions_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(Collections.emptyList());

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getAllSchedulesWithPositions();

        // Assert
        assertTrue(result.isEmpty());
        verify(positionSchedulingRepository, times(1)).findAllWithEmployeeAndPosition();
    }

    @Test
    void getAllSchedulesWithPositions_MultipleSchedules_AllMapped() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(testScheduling, testSchedulingNoPosition));

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getAllSchedulesWithPositions();

        // Assert
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
        assertEquals("Jane Smith", result.get(1).getFullName());
        assertEquals("Cashier", result.get(0).getPositionName());
        assertEquals("Unassigned", result.get(1).getPositionName());
    }

    @Test
    void getAllSchedulesWithPositions_EmployeeIsNull_HandlesGracefully() {
        // Arrange
        Scheduling schedulingWithNullEmployee = new Scheduling();
        schedulingWithNullEmployee.setSchedulingId(3L);
        schedulingWithNullEmployee.setEmployee(null);
        schedulingWithNullEmployee.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        schedulingWithNullEmployee.setStartTime(LocalTime.of(10, 0));
        schedulingWithNullEmployee.setEndTime(LocalTime.of(18, 0));

        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenReturn(List.of(schedulingWithNullEmployee));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getAllSchedulesWithPositions().get(0);

        // Assert
        assertNull(dto.getEmployeeId());
        assertEquals("No Employee", dto.getFullName());
        assertEquals("Unassigned", dto.getPositionName());
        assertEquals(BigDecimal.ZERO, dto.getHourlyWage());
        assertEquals(3L, dto.getScheduleId());
    }

    // ========== GET SCHEDULES BY EMPLOYEE ID TESTS ==========

    @Test
    void getSchedulesByEmployeeId_ReturnsMappedDTOs() {
        // Arrange - Note: Repository returns DTOs directly, not entities
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testScheduleDTO));

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getSchedulesByEmployeeId(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testScheduleDTO, result.get(0));
        verify(positionSchedulingRepository, times(1)).findByEmployeeIdWithPosition(1L);
    }

    @Test
    void getSchedulesByEmployeeId_MapsAllFieldsCorrectly() {
        // Arrange
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testScheduleDTO));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getSchedulesByEmployeeId(1L).get(0);

        // Assert
        assertEquals(1L, dto.getEmployeeId());
        assertEquals("John Doe", dto.getFullName());
        assertEquals("Cashier", dto.getPositionName());
        assertEquals(new BigDecimal("18.50"), dto.getHourlyWage());
        assertEquals(1L, dto.getScheduleId());
        assertEquals(Scheduling.DayOfWeek.MONDAY, dto.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), dto.getStartTime());
        assertEquals(LocalTime.of(17, 0), dto.getEndTime());
    }

    @Test
    void getSchedulesByEmployeeId_NullPosition_DefaultsToUnassignedAndZeroWage() {
        // Arrange - Repository returns DTO with Unassigned position
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(2L))
                .thenReturn(List.of(testScheduleNoPositionDTO));

        // Act
        EmployeeScheduleDTO dto = positionSchedulingService.getSchedulesByEmployeeId(2L).get(0);

        // Assert
        assertEquals("Unassigned", dto.getPositionName());
        assertEquals(BigDecimal.ZERO, dto.getHourlyWage());
    }

    @Test
    void getSchedulesByEmployeeId_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(Collections.emptyList());

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getSchedulesByEmployeeId(1L);

        // Assert
        assertTrue(result.isEmpty());
        verify(positionSchedulingRepository, times(1)).findByEmployeeIdWithPosition(1L);
    }

    @Test
    void getSchedulesByEmployeeId_MultipleSchedules_AllMapped() {
        // Arrange - Create multiple DTOs for the same employee
        EmployeeScheduleDTO mondayShiftDTO = new EmployeeScheduleDTO(
                1L,
                "John Doe",
                "Cashier",
                new BigDecimal("18.50"),
                1L,
                Scheduling.DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        EmployeeScheduleDTO wednesdayShiftDTO = new EmployeeScheduleDTO(
                1L,
                "John Doe",
                "Cashier",
                new BigDecimal("18.50"),
                3L,
                Scheduling.DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );

        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(mondayShiftDTO, wednesdayShiftDTO));

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getSchedulesByEmployeeId(1L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(Scheduling.DayOfWeek.MONDAY, result.get(0).getDayOfWeek());
        assertEquals(Scheduling.DayOfWeek.WEDNESDAY, result.get(1).getDayOfWeek());
    }

    @Test
    void getSchedulesByEmployeeId_RepositoryReturnsCollection_ConvertsToList() {
        // Arrange - Test that Collection return type is handled correctly
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(List.of(testScheduleDTO));

        // Act
        List<EmployeeScheduleDTO> result = positionSchedulingService.getSchedulesByEmployeeId(1L);

        // Assert
        assertInstanceOf(ArrayList.class, result);
        assertEquals(1, result.size());
    }

    
    @Test
    void getSchedulesByEmployeeId_RepositoryReturnsNull_ThrowsNullPointerException() {
        // Arrange
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            positionSchedulingService.getSchedulesByEmployeeId(1L);
        });
        verify(positionSchedulingRepository, times(1)).findByEmployeeIdWithPosition(1L);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void getAllSchedulesWithPositions_RepositoryThrowsException_PropagatesException() {
        // Arrange
        when(positionSchedulingRepository.findAllWithEmployeeAndPosition())
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            positionSchedulingService.getAllSchedulesWithPositions());
        verify(positionSchedulingRepository, times(1)).findAllWithEmployeeAndPosition();
    }

    @Test
    void getSchedulesByEmployeeId_RepositoryThrowsException_PropagatesException() {
        // Arrange
        when(positionSchedulingRepository.findByEmployeeIdWithPosition(1L))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            positionSchedulingService.getSchedulesByEmployeeId(1L));
        verify(positionSchedulingRepository, times(1)).findByEmployeeIdWithPosition(1L);
    }
}