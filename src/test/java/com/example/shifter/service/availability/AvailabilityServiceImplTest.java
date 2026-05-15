package com.example.shifter.service.availability;

import com.example.shifter.dto.availability.CreateAvailabilityRequest.AvailabilitySlot;
import com.example.shifter.model.User;
import com.example.shifter.model.availability.Availability;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.availability.AvailabilityRepository;
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
class AvailabilityServiceImplTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AvailabilityServiceImpl availabilityService;

    private User testEmployee;
    private Availability testAvailability;
    private AvailabilitySlot testSlot;

    @BeforeEach
    void setUp() {
        testEmployee = new User();
        testEmployee.setId(1L);
        testEmployee.setUsername("john.doe");

        testAvailability = new Availability();
        testAvailability.setAvailabilityId(1L);
        testAvailability.setEmployee(testEmployee);
        testAvailability.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        testAvailability.setStartTime(LocalTime.of(9, 0));
        testAvailability.setEndTime(LocalTime.of(17, 0));

        testSlot = new AvailabilitySlot();
        testSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        testSlot.setStartTime("09:00");
        testSlot.setEndTime("17:00");
    }

    // ========== CREATE AVAILABILITY TESTS ==========
    @Test
    void createAvailability_WithValidSlots_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(1);
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.createAvailability(1L, List.of(testSlot));

        // Assert
        verify(userRepository).findById(1L);
        verify(availabilityRepository).deleteByEmployeeId(1L);
        verify(availabilityRepository).saveAll(anyList());
    }

    @Test
    void createAvailability_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.createAvailability(999L, List.of(testSlot)));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    void createAvailability_EmptySlots_ClearsExisting() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(userRepository.existsById(1L)).thenReturn(true); // Add this line!
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(1); // Optional: to verify deletion

        // Act
        availabilityService.createAvailability(1L, Collections.emptyList());

        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).existsById(1L); // Verify this was called
        verify(availabilityRepository).deleteByEmployeeId(1L);
        verify(availabilityRepository, never()).saveAll(any());
    }

    @Test
    void createAvailability_InvalidTimeFormat_ThrowsException() {
        // Arrange
        AvailabilitySlot invalidSlot = new AvailabilitySlot();
        invalidSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        invalidSlot.setStartTime("invalid-time");
        invalidSlot.setEndTime("17:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> availabilityService.createAvailability(1L, List.of(invalidSlot)));
    }

    @Test
    void createAvailability_WithOverlappingNewSlots_ThrowsException() {
        // Arrange
        AvailabilitySlot slot1 = new AvailabilitySlot();
        slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("12:00");

        AvailabilitySlot slot2 = new AvailabilitySlot();
        slot2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot2.setStartTime("11:00"); // Overlaps with slot1
        slot2.setEndTime("14:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.createAvailability(1L, List.of(slot1, slot2)));
        assertTrue(exception.getMessage().contains("Overlap in new schedule"));
    }

    @Test
    void createAvailability_OverlapsWithExisting_ThrowsException() {
        // Arrange
        Availability existing = new Availability();
        existing.setAvailabilityId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> availabilityService.createAvailability(1L, List.of(testSlot)));
    }

    // ========== UPDATE AVAILABILITY TESTS ==========
    @Test
    void updateAvailabilityById_ValidInput_Success() {
        // Arrange
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(testAvailability));
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        availabilityService.updateAvailabilityById(1L,
                Availability.DayOfWeek.TUESDAY,
                "10:00",
                "18:00");

        // Assert
        verify(availabilityRepository).save(testAvailability);
        assertEquals(Availability.DayOfWeek.TUESDAY, testAvailability.getDayOfWeek());
        assertEquals(LocalTime.of(10, 0), testAvailability.getStartTime());
        assertEquals(LocalTime.of(18, 0), testAvailability.getEndTime());
    }

    @Test
    void updateAvailabilityById_AvailabilityNotFound_ThrowsException() {
        // Arrange
        when(availabilityRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.updateAvailabilityById(999L,
                        Availability.DayOfWeek.MONDAY,
                        "09:00",
                        "17:00"));
        assertTrue(exception.getMessage().contains("Availability not found"));
    }

    @Test
    void updateAvailabilityById_InvalidTimeRange_ThrowsException() {
        // Arrange
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(testAvailability));

        // Act & Assert - end time before start time
        assertThrows(IllegalArgumentException.class,
                () -> availabilityService.updateAvailabilityById(1L,
                        Availability.DayOfWeek.MONDAY,
                        "17:00", // Start
                        "09:00") // End - invalid
        );
    }

    @Test
    void updateAvailabilityById_OverlapsWithOtherSlot_ThrowsException() {
        // Arrange
        Availability conflictingSlot = new Availability();
        conflictingSlot.setAvailabilityId(2L);

        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(testAvailability));
        when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflictingSlot));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> availabilityService.updateAvailabilityById(1L,
                        Availability.DayOfWeek.MONDAY,
                        "10:00",
                        "18:00"));
    }

    // ========== DELETE AVAILABILITY TESTS ==========
    @Test
    void deleteAvailabilityById_ValidId_Success() {
        // Arrange
        when(availabilityRepository.existsById(1L)).thenReturn(true);

        // Act
        availabilityService.deleteAvailabilityById(1L);

        // Assert
        verify(availabilityRepository).deleteById(1L);
    }

    @Test
    void deleteAvailabilityById_NotFound_ThrowsException() {
        // Arrange
        when(availabilityRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.deleteAvailabilityById(999L));
        assertTrue(exception.getMessage().contains("Availability not found"));
    }

    // ========== DELETE ALL AVAILABILITIES TESTS ==========
    @Test
    void deleteAllAvailabilitiesOfEmployee_ValidEmployee_Success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(3);

        // Act
        availabilityService.deleteAllAvailabilitiesOfEmployee(1L);

        // Assert
        verify(availabilityRepository).deleteByEmployeeId(1L);
    }

    @Test
    void deleteAllAvailabilitiesOfEmployee_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.deleteAllAvailabilitiesOfEmployee(999L));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    // ========== GET AVAILABILITIES TESTS ==========
    @Test
    void getAllAvailabilities_ReturnsList() {
        // Arrange
        List<Availability> expected = List.of(testAvailability);
        when(availabilityRepository.findAll()).thenReturn(expected);

        // Act
        List<Availability> result = availabilityService.getAllAvailabilities();

        // Assert
        assertEquals(expected, result);
        verify(availabilityRepository).findAll();
    }

    @Test
    void getAvailabilitiesByEmployee_ValidEmployee_ReturnsList() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilityRepository.findByEmployeeId(1L)).thenReturn(List.of(testAvailability));

        // Act
        List<Availability> result = availabilityService.getAvailabilitiesByEmployee(1L);

        // Assert
        assertEquals(1, result.size());
        verify(availabilityRepository).findByEmployeeId(1L);
    }

    @Test
    void getAvailabilitiesByEmployee_EmployeeNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.getAvailabilitiesByEmployee(999L));
        assertTrue(exception.getMessage().contains("Employee not found"));
    }

    @Test
    void getAvailabilitiesByEmployeeAndDay_ValidInput_ReturnsList() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        when(availabilityRepository.findByEmployeeIdAndDayOfWeek(1L, Availability.DayOfWeek.MONDAY))
                .thenReturn(List.of(testAvailability));

        // Act
        List<Availability> result = availabilityService.getAvailabilitiesByEmployeeAndDay(
                1L, Availability.DayOfWeek.MONDAY);

        // Assert
        assertEquals(1, result.size());
        verify(availabilityRepository).findByEmployeeIdAndDayOfWeek(1L, Availability.DayOfWeek.MONDAY);
    }

    @Test
    void getAvailabilityById_ValidId_ReturnsAvailability() {
        // Arrange
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(testAvailability));

        // Act
        Availability result = availabilityService.getAvailabilityById(1L);

        // Assert
        assertEquals(testAvailability, result);
        verify(availabilityRepository).findById(1L);
    }

    @Test
    void getAvailabilityById_NotFound_ThrowsException() {
        // Arrange
        when(availabilityRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.getAvailabilityById(999L));
        assertTrue(exception.getMessage().contains("Availability not found"));
    }

    // ========== UTILITY METHODS TESTS ==========
    @Test
    void hasAvailabilities_EmployeeHasAvailabilities_ReturnsTrue() {
        // Arrange
        when(availabilityRepository.countByEmployeeId(1L)).thenReturn(3L);

        // Act
        boolean result = availabilityService.hasAvailabilities(1L);

        // Assert
        assertTrue(result);
        verify(availabilityRepository).countByEmployeeId(1L);
    }

    @Test
    void hasAvailabilities_EmployeeHasNoAvailabilities_ReturnsFalse() {
        // Arrange
        when(availabilityRepository.countByEmployeeId(1L)).thenReturn(0L);

        // Act
        boolean result = availabilityService.hasAvailabilities(1L);

        // Assert
        assertFalse(result);
    }

    @Test
    void getAvailableEmployeesByDay_ReturnsEmployees() {
        // Arrange
        List<User> expected = List.of(testEmployee);
        when(availabilityRepository.findAvailableEmployeesByDay(Availability.DayOfWeek.MONDAY))
                .thenReturn(expected);

        // Act
        List<User> result = availabilityService.getAvailableEmployeesByDay(Availability.DayOfWeek.MONDAY);

        // Assert
        assertEquals(expected, result);
        verify(availabilityRepository).findAvailableEmployeesByDay(Availability.DayOfWeek.MONDAY);
    }

    @Test
    void validateTimeRange_ValidRange_NoException() {
        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> availabilityService.validateTimeRange("09:00", "17:00"));
    }

    @Test
    void validateTimeRange_InvalidRange_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.validateTimeRange("17:00", "09:00"));
        assertTrue(exception.getMessage().contains("must be after"));
    }

    @Test
    void parseTimeString_ValidTime_ReturnsLocalTime() {
        // Act
        LocalTime result = availabilityService.parseTimeString("14:30");

        // Assert
        assertEquals(LocalTime.of(14, 30), result);
    }

    // ========== PRIVATE HELPER METHOD TESTS (via public methods) ==========
    @Test
    void validateTimeRange_TooShortDuration_ThrowsException() {
        // Act & Assert - Less than 30 minutes
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> availabilityService.validateTimeRange("09:00", "09:15"));
        assertTrue(exception.getMessage().contains("Minimum availability duration"));
    }

    @Test
    void checkForBatchOverlaps_NoOverlaps_NoException() {
        // Arrange
        Availability availability1 = new Availability();
        availability1.setEmployee(testEmployee);
        availability1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        availability1.setStartTime(LocalTime.of(9, 0));
        availability1.setEndTime(LocalTime.of(12, 0));

        Availability availability2 = new Availability();
        availability2.setEmployee(testEmployee);
        availability2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        availability2.setStartTime(LocalTime.of(13, 0));
        availability2.setEndTime(LocalTime.of(17, 0));

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> {
            // Use reflection or test through createAvailability
            when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
            when(availabilityRepository.deleteByEmployeeId(1L)).thenReturn(1);
            when(availabilityRepository.findOverlappingAvailabilities(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            AvailabilitySlot slot1 = new AvailabilitySlot();
            slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
            slot1.setStartTime("09:00");
            slot1.setEndTime("12:00");

            AvailabilitySlot slot2 = new AvailabilitySlot();
            slot2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
            slot2.setStartTime("13:00");
            slot2.setEndTime("17:00");

            availabilityService.createAvailability(1L, List.of(slot1, slot2));
        });
    }

    @Test
    void parseAndValidateSlots_MixedValidAndInvalid_ThrowsExceptionWithAllErrors() {
        // Arrange
        AvailabilitySlot validSlot = new AvailabilitySlot();
        validSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        validSlot.setStartTime("09:00");
        validSlot.setEndTime("17:00");

        AvailabilitySlot invalidSlot = new AvailabilitySlot();
        invalidSlot.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        invalidSlot.setStartTime("invalid");
        invalidSlot.setEndTime("17:00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
                    availabilityService.createAvailability(1L, List.of(validSlot, invalidSlot));
                });

        assertTrue(exception.getMessage().contains("Batch validation failed"));
        assertTrue(exception.getMessage().contains("Slot 2"));
    }
}