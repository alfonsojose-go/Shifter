package com.example.shifter.service.availability;

import com.example.shifter.dto.availability.CreateAvailabilityRequest.AvailabilitySlot;
import com.example.shifter.model.User;
import com.example.shifter.model.availability.Availability;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.availability.AvailabilityRepository;
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
class AvailabilityServiceImpl_createAvailabilityTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AvailabilityServiceImpl availabilityService;

    private User testEmployee;
    private AvailabilitySlot mondaySlot;
    private AvailabilitySlot tuesdaySlot;
    private AvailabilitySlot wednesdaySlot;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setUsername("john.doe");

        // Single slot for Monday
        mondaySlot = new AvailabilitySlot();
        mondaySlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        mondaySlot.setStartTime("09:00");
        mondaySlot.setEndTime("17:00");

        // Another slot for Tuesday
        tuesdaySlot = new AvailabilitySlot();
        tuesdaySlot.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        tuesdaySlot.setStartTime("10:00");
        tuesdaySlot.setEndTime("18:00");

        // Slot for Wednesday
        wednesdaySlot = new AvailabilitySlot();
        wednesdaySlot.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        wednesdaySlot.setStartTime("08:00");
        wednesdaySlot.setEndTime("16:00");
    }

    // ========== SINGLE REQUEST TESTS ==========
    @Test
    void createAvailability_SingleSlotRequest_Success() {
        // Arrange - Single slot (like part-time employee)
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act - Single slot in list
        availabilityService.createAvailability(1L, List.of(mondaySlot));

        // Assert
        verify(userRepository).findById(1L);
        verify(availabilityRepository).deleteByEmployeeId(1L);

        // Use ArgumentCaptor to capture and verify
        ArgumentCaptor<List<Availability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());

        List<Availability> savedAvailabilities = captor.getValue();
        assertEquals(1, savedAvailabilities.size(),
                "Should save exactly 1 availability slot");

        // Additional useful assertions:
        assertEquals(Availability.DayOfWeek.MONDAY, savedAvailabilities.get(0).getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), savedAvailabilities.get(0).getStartTime());
        assertEquals(testEmployee, savedAvailabilities.get(0).getEmployee());
    }

    @Test
    void createAvailability_SingleSlotWithNoPreviousData_CreatesSuccessfully() {
        // Arrange - Employee has no existing availabilities
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0); // No previous data
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, List.of(mondaySlot));

        // Assert
        verify(availabilityRepository).deleteByEmployeeId(1L);
        verify(availabilityRepository).saveAll(anyList());
    }

    @Test
    void createAvailability_SingleSlotReplacesExisting_DeletesOldFirst() {
        // Arrange - Employee had existing Monday slot, now replacing with new Monday
        // slot
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(2); // Had 2 existing slots
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, List.of(mondaySlot));

        // Assert - Should delete old (2 slots) and save new (1 slot)
        verify(availabilityRepository).deleteByEmployeeId(1L);

        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Availability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    // ========== BATCH REQUEST TESTS ==========
    @Test
    void createAvailability_BatchRequest_MultipleDays_Success() {
        // Arrange - Full-time employee with weekly schedule
        List<AvailabilitySlot> weeklySchedule = Arrays.asList(
                mondaySlot, // Monday 9-5
                tuesdaySlot, // Tuesday 10-6
                wednesdaySlot // Wednesday 8-4
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, weeklySchedule);

        // Assert
        verify(availabilityRepository).deleteByEmployeeId(1L);

        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Availability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
    }

    @Test
    void createAvailability_BatchRequest_SameDayMultipleSlots_Success() {
        // Arrange - Split shifts on same day (e.g., morning and evening)
        AvailabilitySlot morningSlot = new AvailabilitySlot();
        morningSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        morningSlot.setStartTime("08:00");
        morningSlot.setEndTime("12:00");

        AvailabilitySlot afternoonSlot = new AvailabilitySlot();
        afternoonSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        afternoonSlot.setStartTime("13:00");
        afternoonSlot.setEndTime("17:00");

        List<AvailabilitySlot> splitShift = Arrays.asList(morningSlot, afternoonSlot);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, splitShift);

        // Assert - Should save 2 slots for same day
        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Availability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());

        List<Availability> savedAvailabilities = captor.getValue();
        assertEquals(2, savedAvailabilities.size());
        assertEquals(Availability.DayOfWeek.MONDAY, savedAvailabilities.get(0).getDayOfWeek());
        assertEquals(Availability.DayOfWeek.MONDAY, savedAvailabilities.get(1).getDayOfWeek());
    }

    @Test
    void createAvailability_BatchRequest_ComplexWeeklySchedule_Success() {
        // Arrange - Complex schedule with different days and times
        AvailabilitySlot mondayEarly = new AvailabilitySlot();
        mondayEarly.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        mondayEarly.setStartTime("06:00");
        mondayEarly.setEndTime("14:00");

        AvailabilitySlot tuesdayLate = new AvailabilitySlot();
        tuesdayLate.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        tuesdayLate.setStartTime("14:00");
        tuesdayLate.setEndTime("22:00");

        AvailabilitySlot wednesdaySplit1 = new AvailabilitySlot();
        wednesdaySplit1.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        wednesdaySplit1.setStartTime("08:00");
        wednesdaySplit1.setEndTime("12:00");

        AvailabilitySlot wednesdaySplit2 = new AvailabilitySlot();
        wednesdaySplit2.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        wednesdaySplit2.setStartTime("13:00");
        wednesdaySplit2.setEndTime("17:00");

        // Thursday off (not included)

        AvailabilitySlot fridayMorning = new AvailabilitySlot();
        fridayMorning.setDayOfWeek(Availability.DayOfWeek.FRIDAY);
        fridayMorning.setStartTime("07:00");
        fridayMorning.setEndTime("15:00");

        List<AvailabilitySlot> complexSchedule = Arrays.asList(
                mondayEarly, tuesdayLate, wednesdaySplit1, wednesdaySplit2, fridayMorning);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(5); // Had old schedule
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, complexSchedule);

        // Assert
        verify(availabilityRepository).deleteByEmployeeId(1L);
        ArgumentCaptor<List<Availability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());
        assertEquals(5, captor.getValue().size());
    }

    @Test
    void createAvailability_BatchRequest_EmptyList_ClearsExistingOnly() {
        // Arrange - Employee clearing their entire schedule
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.existsById(1L)).thenReturn(true); // ADD THIS LINE
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(3);

        // Act - Empty list means "clear all availability"
        availabilityService.createAvailability(1L, Collections.emptyList());

        // Assert - Should delete existing but NOT save anything new
        verify(userRepository).findById(1L);
        verify(userRepository).existsById(1L); // VERIFY THIS WAS CALLED
        verify(availabilityRepository).deleteByEmployeeId(1L);
        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void createAvailability_BatchRequest_NullList_ClearsExistingOnly() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(2);

        // Act - Null list also means "clear all"
        availabilityService.createAvailability(1L, null);

        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).existsById(1L);
        verify(availabilityRepository).deleteByEmployeeId(1L);
        verify(availabilityRepository, never()).saveAll(any());
    }

    // ========== BATCH VALIDATION TESTS ==========
    @Test
    void createAvailability_BatchRequest_OverlapWithinBatch_ThrowsException() {
        // Arrange - Two slots overlap on same day
        AvailabilitySlot slot1 = new AvailabilitySlot();
        slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("13:00");

        AvailabilitySlot slot2 = new AvailabilitySlot();
        slot2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot2.setStartTime("11:00"); // Overlaps with slot1 (11-13 overlaps with 9-13)
        slot2.setEndTime("15:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.createAvailability(1L, Arrays.asList(slot1, slot2)));
        assertTrue(exception.getMessage().contains("Overlap in new schedule"));
        assertTrue(exception.getMessage().contains("MONDAY"));
    }

    @Test
    void createAvailability_BatchRequest_MultipleOverlapsDifferentDays_NoException() {
        // Arrange - No overlaps because different days
        AvailabilitySlot mondaySlot = new AvailabilitySlot();
        mondaySlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        mondaySlot.setStartTime("09:00");
        mondaySlot.setEndTime("17:00");

        AvailabilitySlot tuesdaySlot = new AvailabilitySlot();
        tuesdaySlot.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        tuesdaySlot.setStartTime("09:00"); // Same time but different day - OK
        tuesdaySlot.setEndTime("17:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act & Assert - Should succeed
        assertDoesNotThrow(() -> availabilityService.createAvailability(1L, Arrays.asList(mondaySlot, tuesdaySlot)));
    }

    @Test
    void createAvailability_BatchRequest_AdjacentTimesSameDay_NoOverlap() {
        // Arrange - Adjacent but not overlapping: 9-12 and 12-3
        AvailabilitySlot morning = new AvailabilitySlot();
        morning.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        morning.setStartTime("09:00");
        morning.setEndTime("12:00");

        AvailabilitySlot afternoon = new AvailabilitySlot();
        afternoon.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        afternoon.setStartTime("12:00"); // Starts exactly when morning ends - OK
        afternoon.setEndTime("15:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act & Assert - Should succeed (adjacent is not overlapping)
        assertDoesNotThrow(() -> availabilityService.createAvailability(1L, Arrays.asList(morning, afternoon)));
    }

    // ========== MIXED SINGLE/BATCH ERROR HANDLING ==========
    @Test
    void createAvailability_BatchRequest_PartialInvalidTimes_ThrowsBatchError() {
        // Arrange - One valid, one invalid time format
        AvailabilitySlot validSlot = new AvailabilitySlot();
        validSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        validSlot.setStartTime("09:00");
        validSlot.setEndTime("17:00");

        AvailabilitySlot invalidSlot = new AvailabilitySlot();
        invalidSlot.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        invalidSlot.setStartTime("25:00"); // Invalid hour
        invalidSlot.setEndTime("17:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.createAvailability(1L, Arrays.asList(validSlot, invalidSlot)));
        assertTrue(exception.getMessage().contains("Batch validation failed"));
        assertTrue(exception.getMessage().contains("Slot 2"));
    }

    @Test
    void createAvailability_BatchRequest_MultipleValidationErrors_AggregatesAll() {
        // Arrange - Multiple slots with different errors
        AvailabilitySlot slot1 = new AvailabilitySlot();
        slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot1.setStartTime("invalid");
        slot1.setEndTime("17:00");

        AvailabilitySlot slot2 = new AvailabilitySlot();
        slot2.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        slot2.setStartTime("14:00");
        slot2.setEndTime("10:00"); // End before start

        AvailabilitySlot slot3 = new AvailabilitySlot();
        slot3.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        slot3.setStartTime("09:00");
        slot3.setEndTime("09:15"); // Too short (less than 30 min)

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.createAvailability(1L, Arrays.asList(slot1, slot2, slot3)));

        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("Batch validation failed"));
        assertTrue(errorMessage.contains("Slot 1"));
        assertTrue(errorMessage.contains("Slot 2"));
        assertTrue(errorMessage.contains("Slot 3"));
    }

    // ========== PERFORMANCE/EDGE CASES ==========
    @Test
    void createAvailability_LargeBatchRequest_HandlesManySlots() {
        // Arrange - Simulating a very complex weekly schedule
        List<AvailabilitySlot> largeBatch = new ArrayList<>();

        // Create 20 slots (simulating detailed scheduling)
        for (int i = 0; i < 5; i++) { // 5 days
            for (int j = 0; j < 4; j++) { // 4 slots per day
                AvailabilitySlot slot = new AvailabilitySlot();
                slot.setDayOfWeek(Availability.DayOfWeek.values()[i % 7]); // Cycle through days
                slot.setStartTime(String.format("%02d:00", 8 + j * 2)); // 8, 10, 12, 14
                slot.setEndTime(String.format("%02d:00", 10 + j * 2)); // 10, 12, 14, 16
                largeBatch.add(slot);
            }
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, largeBatch);

        // Assert - Should handle 20 slots
        ArgumentCaptor<List<Availability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());
        assertEquals(20, captor.getValue().size());

    }

    @Test
    void createAvailability_SingleSlotThenBatch_ReplacesCompletely() {
        // Test scenario: First set single slot, then replace with batch

        // First: Set single Monday slot
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(0).thenReturn(1);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act - Step 1: Single slot
        availabilityService.createAvailability(1L, List.of(mondaySlot));

        // Verify step 1
        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Availability>> captor1 = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository, times(1)).saveAll(captor1.capture());
        assertEquals(1, captor1.getValue().size());

        // Reset mocks for clarity
        reset(availabilityRepository);
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(1);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Step 2: Replace with batch (full week)
        List<AvailabilitySlot> fullWeek = Arrays.asList(
                mondaySlot, tuesdaySlot, wednesdaySlot);

        availabilityService.createAvailability(1L, fullWeek);

        // Assert - Should delete old (1 slot) and save new batch (3 slots)
        verify(availabilityRepository).deleteByEmployeeId(1L);

        // FIX: Use ArgumentCaptor
        ArgumentCaptor<List<Availability>> captor2 = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor2.capture());
        assertEquals(3, captor2.getValue().size());
    }
}