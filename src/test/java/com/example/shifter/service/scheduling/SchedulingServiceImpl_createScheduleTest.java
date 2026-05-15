package com.example.shifter.service.scheduling;

import com.example.shifter.dto.scheduling.CreateSchedulingRequest.SchedulingSlot;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.scheduling.SchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceImpl_createScheduleTest {

    @Mock
    private SchedulingRepository schedulingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private User testEmployee;
    private SchedulingSlot mondaySlot;
    private SchedulingSlot tuesdaySlot;
    private SchedulingSlot wednesdaySlot;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setUsername("john.doe");

        // Single slot for Monday
        mondaySlot = new SchedulingSlot();
        mondaySlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        mondaySlot.setStartTime("09:00");
        mondaySlot.setEndTime("17:00");

        // Another slot for Tuesday
        tuesdaySlot = new SchedulingSlot();
        tuesdaySlot.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        tuesdaySlot.setStartTime("10:00");
        tuesdaySlot.setEndTime("18:00");

        // Slot for Wednesday
        wednesdaySlot = new SchedulingSlot();
        wednesdaySlot.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        wednesdaySlot.setStartTime("08:00");
        wednesdaySlot.setEndTime("16:00");
    }

    // ========== SINGLE REQUEST TESTS ==========
    @Test
    void createSchedule_SingleSlotRequest_Success() {
        // Arrange - Single slot (like part-time employee)
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act - Single slot in list
        schedulingService.createSchedule(1L, List.of(mondaySlot));

        // Assert
        verify(userRepository).findById(1L);
        verify(schedulingRepository).deleteByEmployeeId(1L);

        // Use ArgumentCaptor to capture and verify
        ArgumentCaptor<List<Scheduling>> captor = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor.capture());

        List<Scheduling> savedScheduling = captor.getValue();
        assertEquals(1, savedScheduling.size(),
                "Should save exactly 1 scheduling slot");

        // Additional useful assertions:
        assertEquals(Scheduling.DayOfWeek.MONDAY, savedScheduling.get(0).getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), savedScheduling.get(0).getStartTime());
        assertEquals(testEmployee, savedScheduling.get(0).getEmployee());
    }

    @Test
    void createSchedule_SingleSlotWithNoPreviousData_CreatesSuccessfully() {
        // Arrange - Employee has no existing scheduling
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0); // No previous data
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, List.of(mondaySlot));

        // Assert
        verify(schedulingRepository).deleteByEmployeeId(1L);
        verify(schedulingRepository).saveAll(anyList());
    }

    @Test
    void createSchedule_SingleSlotReplacesExisting_DeletesOldFirst() {
        // Arrange - Employee had existing Monday slot, now replacing with new Monday
        // slot
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(2); // Had 2 existing slots
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, List.of(mondaySlot));

        // Assert - Should delete old (2 slots) and save new (1 slot)
        verify(schedulingRepository).deleteByEmployeeId(1L);

        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Scheduling>> captor = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    // ========== BATCH REQUEST TESTS ==========
    @Test
    void createSchedule_BatchRequest_MultipleDays_Success() {
        // Arrange - Full-time employee with weekly schedule
        List<SchedulingSlot> weeklySchedule = Arrays.asList(
                mondaySlot, // Monday 9-5
                tuesdaySlot, // Tuesday 10-6
                wednesdaySlot // Wednesday 8-4
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, weeklySchedule);

        // Assert
        verify(schedulingRepository).deleteByEmployeeId(1L);

        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Scheduling>> captor = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
    }

    @Test
    void createSchedule_BatchRequest_SameDayMultipleSlots_Success() {
        // Arrange - Split shifts on same day (e.g., morning and evening)
        SchedulingSlot morningSlot = new SchedulingSlot();
        morningSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        morningSlot.setStartTime("08:00");
        morningSlot.setEndTime("12:00");

        SchedulingSlot afternoonSlot = new SchedulingSlot();
        afternoonSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        afternoonSlot.setStartTime("13:00");
        afternoonSlot.setEndTime("17:00");

        List<SchedulingSlot> splitShift = Arrays.asList(morningSlot, afternoonSlot);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, splitShift);

        // Assert - Should save 2 slots for same day
        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Scheduling>> captor = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor.capture());

        List<Scheduling> savedScheduling = captor.getValue();
        assertEquals(2, savedScheduling.size());
        assertEquals(Scheduling.DayOfWeek.MONDAY, savedScheduling.get(0).getDayOfWeek());
        assertEquals(Scheduling.DayOfWeek.MONDAY, savedScheduling.get(1).getDayOfWeek());
    }

    @Test
    void createSchedule_BatchRequest_ComplexWeeklySchedule_Success() {
        // Arrange - Complex schedule with different days and times
        SchedulingSlot mondayEarly = new SchedulingSlot();
        mondayEarly.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        mondayEarly.setStartTime("06:00");
        mondayEarly.setEndTime("14:00");

        SchedulingSlot tuesdayLate = new SchedulingSlot();
        tuesdayLate.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        tuesdayLate.setStartTime("14:00");
        tuesdayLate.setEndTime("22:00");

        SchedulingSlot wednesdaySplit1 = new SchedulingSlot();
        wednesdaySplit1.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        wednesdaySplit1.setStartTime("08:00");
        wednesdaySplit1.setEndTime("12:00");

        SchedulingSlot wednesdaySplit2 = new SchedulingSlot();
        wednesdaySplit2.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        wednesdaySplit2.setStartTime("13:00");
        wednesdaySplit2.setEndTime("17:00");

        // Thursday off (not included)

        SchedulingSlot fridayMorning = new SchedulingSlot();
        fridayMorning.setDayOfWeek(Scheduling.DayOfWeek.FRIDAY);
        fridayMorning.setStartTime("07:00");
        fridayMorning.setEndTime("15:00");

        List<SchedulingSlot> complexSchedule = Arrays.asList(
                mondayEarly, tuesdayLate, wednesdaySplit1, wednesdaySplit2, fridayMorning);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(5); // Had old schedule
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, complexSchedule);

        // Assert
        verify(schedulingRepository).deleteByEmployeeId(1L);
        ArgumentCaptor<List<Scheduling>> captor = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor.capture());
        assertEquals(5, captor.getValue().size());
    }

    @Test
    void createSchedule_BatchRequest_EmptyList_ClearsExistingOnly() {
        // Arrange - Employee clearing their entire schedule
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.existsById(1L)).thenReturn(true); // ADD THIS LINE
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(3);

        // Act - Empty list means "clear all scheduling"
        schedulingService.createSchedule(1L, Collections.emptyList());

        // Assert - Should delete existing but NOT save anything new
        verify(userRepository).findById(1L);
        verify(userRepository).existsById(1L); // VERIFY THIS WAS CALLED
        verify(schedulingRepository).deleteByEmployeeId(1L);
        verify(schedulingRepository, never()).saveAll(any());
    }

    @Test
    void createSchedule_BatchRequest_NullList_ClearsExistingOnly() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(2);

        // Act - Null list also means "clear all"
        schedulingService.createSchedule(1L, null);

        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).existsById(1L);
        verify(schedulingRepository).deleteByEmployeeId(1L);
        verify(schedulingRepository, never()).saveAll(any());
    }

    // ========== BATCH VALIDATION TESTS ==========
    @Test
    void createSchedule_BatchRequest_OverlapWithinBatch_ThrowsException() {
        // Arrange - Two slots overlap on same day
        SchedulingSlot slot1 = new SchedulingSlot();
        slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("13:00");

        SchedulingSlot slot2 = new SchedulingSlot();
        slot2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot2.setStartTime("11:00"); // Overlaps with slot1 (11-13 overlaps with 9-13)
        slot2.setEndTime("15:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.createSchedule(1L, Arrays.asList(slot1, slot2)));
        assertTrue(exception.getMessage().contains("Overlap in new schedule"));
        assertTrue(exception.getMessage().contains("MONDAY"));
    }

    @Test
    void createSchedule_BatchRequest_MultipleOverlapsDifferentDays_NoException() {
        // Arrange - No overlaps because different days
        SchedulingSlot mondaySlot = new SchedulingSlot();
        mondaySlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        mondaySlot.setStartTime("09:00");
        mondaySlot.setEndTime("17:00");

        SchedulingSlot tuesdaySlot = new SchedulingSlot();
        tuesdaySlot.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        tuesdaySlot.setStartTime("09:00"); // Same time but different day - OK
        tuesdaySlot.setEndTime("17:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act & Assert - Should succeed
        assertDoesNotThrow(() -> schedulingService.createSchedule(1L, Arrays.asList(mondaySlot, tuesdaySlot)));
    }

    @Test
    void createSchedule_BatchRequest_AdjacentTimesSameDay_NoOverlap() {
        // Arrange - Adjacent but not overlapping: 9-12 and 12-3
        SchedulingSlot morning = new SchedulingSlot();
        morning.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        morning.setStartTime("09:00");
        morning.setEndTime("12:00");

        SchedulingSlot afternoon = new SchedulingSlot();
        afternoon.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        afternoon.setStartTime("12:00"); // Starts exactly when morning ends - OK
        afternoon.setEndTime("15:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act & Assert - Should succeed (adjacent is not overlapping)
        assertDoesNotThrow(() -> schedulingService.createSchedule(1L, Arrays.asList(morning, afternoon)));
    }

    // ========== MIXED SINGLE/BATCH ERROR HANDLING ==========
    @Test
    void createSchedule_BatchRequest_PartialInvalidTimes_ThrowsBatchError() {
        // Arrange - One valid, one invalid time format
        SchedulingSlot validSlot = new SchedulingSlot();
        validSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        validSlot.setStartTime("09:00");
        validSlot.setEndTime("17:00");

        SchedulingSlot invalidSlot = new SchedulingSlot();
        invalidSlot.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        invalidSlot.setStartTime("25:00"); // Invalid hour
        invalidSlot.setEndTime("17:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.createSchedule(1L, Arrays.asList(validSlot, invalidSlot)));
        assertTrue(exception.getMessage().contains("Batch validation failed"));
        assertTrue(exception.getMessage().contains("Slot 2"));
    }

    @Test
    void createSchedule_BatchRequest_MultipleValidationErrors_AggregatesAll() {
        // Arrange - Multiple slots with different errors
        SchedulingSlot slot1 = new SchedulingSlot();
        slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot1.setStartTime("invalid");
        slot1.setEndTime("17:00");

        SchedulingSlot slot2 = new SchedulingSlot();
        slot2.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        slot2.setStartTime("14:00");
        slot2.setEndTime("10:00"); // End before start

        SchedulingSlot slot3 = new SchedulingSlot();
        slot3.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        slot3.setStartTime("09:00");
        slot3.setEndTime("09:15"); // Too short (less than 30 min)

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.createSchedule(1L, Arrays.asList(slot1, slot2, slot3)));

        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("Batch validation failed"));
        assertTrue(errorMessage.contains("Slot 1"));
        assertTrue(errorMessage.contains("Slot 2"));
        assertTrue(errorMessage.contains("Slot 3"));
    }

    // ========== PERFORMANCE/EDGE CASES ==========
    @Test
    void createSchedule_LargeBatchRequest_HandlesManySlots() {
        // Arrange - Simulating a very complex weekly schedule
        List<SchedulingSlot> largeBatch = new ArrayList<>();

        // Create 20 slots (simulating detailed scheduling)
        for (int i = 0; i < 5; i++) { // 5 days
            for (int j = 0; j < 4; j++) { // 4 slots per day
                SchedulingSlot slot = new SchedulingSlot();
                slot.setDayOfWeek(Scheduling.DayOfWeek.values()[i % 7]); // Cycle through days
                slot.setStartTime(String.format("%02d:00", 8 + j * 2)); // 8, 10, 12, 14
                slot.setEndTime(String.format("%02d:00", 10 + j * 2)); // 10, 12, 14, 16
                largeBatch.add(slot);
            }
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        schedulingService.createSchedule(1L, largeBatch);

        // Assert - Should handle 20 slots
        ArgumentCaptor<List<Scheduling>> captor = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor.capture());
        assertEquals(20, captor.getValue().size());

    }

    @Test
    void createSchedule_SingleSlotThenBatch_ReplacesCompletely() {
        // Test scenario: First set single slot, then replace with batch

        // First: Set single Monday slot
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(0).thenReturn(1);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act - Step 1: Single slot
        schedulingService.createSchedule(1L, List.of(mondaySlot));

        // Verify step 1
        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Scheduling>> captor1 = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository, times(1)).saveAll(captor1.capture());
        assertEquals(1, captor1.getValue().size());

        // Reset mocks for clarity
        reset(schedulingRepository);
        when(schedulingRepository.deleteByEmployeeId(1L)).thenReturn(1);
        when(schedulingRepository.findOverlappingSchedules(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Step 2: Replace with batch (full week)
        List<SchedulingSlot> fullWeek = Arrays.asList(
                mondaySlot, tuesdaySlot, wednesdaySlot);

        schedulingService.createSchedule(1L, fullWeek);

        // Assert - Should delete old (1 slot) and save new batch (3 slots)
        verify(schedulingRepository).deleteByEmployeeId(1L);

        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Scheduling>> captor2 = ArgumentCaptor.forClass(List.class);
        verify(schedulingRepository).saveAll(captor2.capture());
        assertEquals(3, captor2.getValue().size());
    }
}