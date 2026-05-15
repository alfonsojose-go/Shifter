package com.example.shifter.service.scheduling;

import com.example.shifter.dto.scheduling.CreateSchedulingRequest.SchedulingSlot;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.scheduling.SchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceImplTest {

    @Mock
    private SchedulingRepository schedulingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private User testEmployee;
    private Scheduling testScheduling;
    private SchedulingSlot testSlot;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setUsername("john.doe");

        testScheduling = new Scheduling();
        testScheduling.setSchedulingId(1L);
        testScheduling.setEmployee(testEmployee);
        testScheduling.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        testScheduling.setStartTime(LocalTime.of(9, 0));
        testScheduling.setEndTime(LocalTime.of(17, 0));

        testSlot = new SchedulingSlot();
        testSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        testSlot.setStartTime("09:00");
        testSlot.setEndTime("17:00");
    }

    // ========== CREATE SCHEDULING TESTS ==========
    @Test
    void createSchedule_WithValidSlots_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(1);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, List.of(testSlot));

        // Assert
        verify(userRepository).findById(1L);
        verify(schedulingRepository).deleteByEmployeeId(1L);
        verify(schedulingRepository).saveAll(anyList());
    }

    @Test
    void createSchedule_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.createSchedule(999L, List.of(testSlot)));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    void createSchedule_EmptySlots_ClearsExisting() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.existsById(1L)).thenReturn(true); // Add this line!
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(1); // Optional: to verify deletion

        // Act
        schedulingService.createSchedule(1L, Collections.emptyList());

        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).existsById(1L); // Verify this was called
        verify(schedulingRepository).deleteByEmployeeId(1L);
        verify(schedulingRepository, never()).saveAll(any());
    }

    @Test
    void createSchedule_InvalidTimeFormat_ThrowsException() {
        // Arrange
        SchedulingSlot invalidSlot = new SchedulingSlot();
        invalidSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        invalidSlot.setStartTime("invalid-time");
        invalidSlot.setEndTime("17:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> schedulingService.createSchedule(1L, List.of(invalidSlot)));
    }

    @Test
    void createSchedule_WithOverlappingNewSlots_ThrowsException() {
        // Arrange
        SchedulingSlot slot1 = new SchedulingSlot();
        slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("12:00");

        SchedulingSlot slot2 = new SchedulingSlot();
        slot2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot2.setStartTime("11:00"); // Overlaps with slot1
        slot2.setEndTime("14:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.createSchedule(1L, List.of(slot1, slot2)));
        assertTrue(exception.getMessage().contains("Overlap in new schedule"));
    }

    @Test
    void createSchedule_OverlapsWithExisting_ThrowsException() {
        // Arrange
        Scheduling existing = new Scheduling();
        existing.setSchedulingId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> schedulingService.createSchedule(1L, List.of(testSlot)));
    }

    // ========== UPDATE SCHEDULING TESTS ==========
    @Test
    void updateScheduleById_ValidInput_Success() {
        // Arrange
        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(testScheduling));
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.updateScheduleById(1L,
                Scheduling.DayOfWeek.TUESDAY,
                "10:00",
                "18:00");

        // Assert
        verify(schedulingRepository).save(testScheduling);
        assertEquals(Scheduling.DayOfWeek.TUESDAY, testScheduling.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), testScheduling.getStartTime());
        assertEquals(LocalTime.of(18, 0), testScheduling.getEndTime());
    }

    @Test
    void updateScheduleById_SchedulingNotFound_ThrowsException() {
        // Arrange
        when(schedulingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.updateScheduleById(999L,
                        Scheduling.DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"));
        assertTrue(exception.getMessage().contains("Scheduling not found"));
    }

    @Test
    void updateScheduleById_InvalidTimeRange_ThrowsException() {
        // Arrange
        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(testScheduling));

        // Act & Assert - end time before start time
        assertThrows(IllegalArgumentException.class,
                () -> schedulingService.updateScheduleById(1L,
                        Scheduling.DayOfWeek.MONDAY,
                        "17:00", // Start
                        "09:00") // End - invalid
        );
    }

    @Test
    void updateScheduleById_OverlapsWithOtherSlot_ThrowsException() {
        // Arrange
        Scheduling conflictingSlot = new Scheduling();
        conflictingSlot.setSchedulingId(2L);

        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(testScheduling));
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflictingSlot));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> schedulingService.updateScheduleById(1L,
                        Scheduling.DayOfWeek.MONDAY,
                        "10:00",
                        "18:00"));
    }

    // ========== DELETE SCHEDULING TESTS ==========
    @Test
    void deleteScheduleById_ValidId_Success() {
        // Arrange
        when(schedulingRepository.existsById(1L)).thenReturn(true);

        // Act
        schedulingService.deleteScheduleById(1L);

        // Assert
        verify(schedulingRepository).deleteById(1L);
    }

    @Test
    void deleteScheduleById_NotFound_ThrowsException() {
        // Arrange
        when(schedulingRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.deleteScheduleById(999L));
        assertTrue(exception.getMessage().contains("Scheduling not found"));
    }

    // ========== DELETE ALL SCHEDULING TESTS ==========
    @Test
    void deleteAllScheduleOfEmployee_ValidEmployee_Success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(3);

        // Act
        schedulingService.deleteAllScheduleOfEmployee(1L);

        // Assert
        verify(schedulingRepository).deleteByEmployeeId(1L);
    }

    @Test
    void deleteAllScheduleOfEmployee_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.deleteAllScheduleOfEmployee(999L));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    // ========== GET SCHEDULING TESTS ==========
    @Test
    void getAllSchedules_ReturnsList() {
        // Arrange
        List<Scheduling> expected = List.of(testScheduling);
        when(schedulingRepository.findAll()).thenReturn(expected);

        // Act
        List<Scheduling> result = schedulingService.getAllSchedules();

        // Assert
        assertEquals(expected, result);
        verify(schedulingRepository).findAll();
    }

    @Test
    void getSchedulesByEmployee_ValidEmployee_ReturnsList() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        when(schedulingRepository.findByEmployeeId(1L)).thenReturn(List.of(testScheduling));

        // Act
        List<Scheduling> result = schedulingService.getSchedulesByEmployee(1L);

        // Assert
        assertEquals(1, result.size());
        verify(schedulingRepository).findByEmployeeId(1L);
    }

    @Test
    void getSchedulesByEmployee_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.getSchedulesByEmployee(999L));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    void getSchedulesByEmployeeAndDay_ValidInput_ReturnsList() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        when(schedulingRepository.findByEmployeeIdAndDayOfWeek(1L, Scheduling.DayOfWeek.MONDAY))
                .thenReturn(List.of(testScheduling));

        // Act
        List<Scheduling> result = schedulingService.getSchedulesByEmployeeAndDay(
                1L, Scheduling.DayOfWeek.MONDAY);

        // Assert
        assertEquals(1, result.size());
        verify(schedulingRepository).findByEmployeeIdAndDayOfWeek(1L, Scheduling.DayOfWeek.MONDAY);
    }

    @Test
    void getSchedulingById_ValidId_ReturnsScheduling() {
        // Arrange
        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(testScheduling));

        // Act
        Scheduling result = schedulingService.getScheduleById(1L);

        // Assert
        assertEquals(testScheduling, result);
        verify(schedulingRepository).findById(1L);
    }

    @Test
    void getSchedulingById_NotFound_ThrowsException() {
        // Arrange
        when(schedulingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.getScheduleById(999L));
        assertTrue(exception.getMessage().contains("Scheduling not found"));
    }

    // ========== UTILITY METHODS TESTS ==========
    @Test
    void hasScheduling_EmployeeHasScheduling_ReturnsTrue() {
        // Arrange
        when(schedulingRepository.countByEmployeeId(1L)).thenReturn(3L);

        // Act
        boolean result = schedulingService.hasSchedule(1L);

        // Assert
        assertTrue(result);
        verify(schedulingRepository).countByEmployeeId(1L);
    }

    @Test
    void hasScheduling_EmployeeHasNoScheduling_ReturnsFalse() {
        // Arrange
        when(schedulingRepository.countByEmployeeId(1L)).thenReturn(0L);

        // Act
        boolean result = schedulingService.hasSchedule(1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void getAvailableEmployeesByDay_ReturnsEmployees() {
        // Arrange
        List<User> expected = List.of(testEmployee);
        when(schedulingRepository.findAvailableEmployeesByDay(Scheduling.DayOfWeek.MONDAY))
                .thenReturn(expected);

        // Act
        List<User> result = schedulingService.getAvailableEmployeesByDay(Scheduling.DayOfWeek.MONDAY);

        // Assert
        assertEquals(expected, result);
        verify(schedulingRepository).findAvailableEmployeesByDay(Scheduling.DayOfWeek.MONDAY);
    }

    @Test
    void validateTimeRange_ValidRange_NoException() {
        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> schedulingService.validateTimeRange("09:00", "17:00"));
    }

    @Test
    void validateTimeRange_InvalidRange_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.validateTimeRange("17:00", "09:00"));
        assertTrue(exception.getMessage().contains("must be after"));
    }

    @Test
    void parseTimeString_ValidTime_ReturnsLocalTime() {
        // Act
        LocalTime result = schedulingService.parseTimeString("14:30");

        // Assert
        assertEquals(LocalTime.of(14, 30), result);
    }

    // ========== PRIVATE HELPER METHOD TESTS (via public methods) ==========
    @Test
    void validateTimeRange_TooShortDuration_ThrowsException() {
        // Act & Assert - Less than 30 minutes
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.validateTimeRange("09:00", "09:15"));
        assertTrue(exception.getMessage().contains("Minimum scheduling duration"));
    }

    @Test
    void checkForBatchOverlaps_NoOverlaps_NoException() {
        // Arrange
        Scheduling scheduling1 = new Scheduling();
        scheduling1.setEmployee(testEmployee);
        scheduling1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        scheduling1.setStartTime(LocalTime.of(9, 0));
        scheduling1.setEndTime(LocalTime.of(12, 0));

        Scheduling scheduling2 = new Scheduling();
        scheduling2.setEmployee(testEmployee);
        scheduling2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        scheduling2.setStartTime(LocalTime.of(13, 0));
        scheduling2.setEndTime(LocalTime.of(17, 0));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            // Use reflection or test through createSchedule
            when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(1);
            when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            SchedulingSlot slot1 = new SchedulingSlot();
            slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
            slot1.setStartTime("09:00");
            slot1.setEndTime("12:00");

            SchedulingSlot slot2 = new SchedulingSlot();
            slot2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
            slot2.setStartTime("13:00");
            slot2.setEndTime("17:00");

            schedulingService.createSchedule(1L, List.of(slot1, slot2));
        });
    }

    @Test
    void parseAndValidateSlots_MixedValidAndInvalid_ThrowsExceptionWithAllErrors() {
        // Arrange
        SchedulingSlot validSlot = new SchedulingSlot();
        validSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        validSlot.setStartTime("09:00");
        validSlot.setEndTime("17:00");

        SchedulingSlot invalidSlot = new SchedulingSlot();
        invalidSlot.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        invalidSlot.setStartTime("invalid");
        invalidSlot.setEndTime("17:00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
                    schedulingService.createSchedule(1L, List.of(validSlot, invalidSlot));
                });

        assertTrue(exception.getMessage().contains("Batch validation failed"));
        assertTrue(exception.getMessage().contains("Slot 2"));
    }
}