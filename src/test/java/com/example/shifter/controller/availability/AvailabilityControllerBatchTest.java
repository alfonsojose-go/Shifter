package com.example.shifter.controller.availability;

import com.example.shifter.dto.availability.*;
import com.example.shifter.model.User;
import com.example.shifter.model.availability.Availability;
import com.example.shifter.service.availability.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AvailabilityControllerBatchTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AvailabilityController availabilityController;

    private User testUser;
    private CreateAvailabilityRequest.AvailabilitySlot mondaySlot;
    private CreateAvailabilityRequest.AvailabilitySlot tuesdaySlot;
    private CreateAvailabilityRequest.AvailabilitySlot wednesdaySlot;

    @BeforeEach
    void setUp() {
        // Setup User
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");

        // Setup test slots
        mondaySlot = new CreateAvailabilityRequest.AvailabilitySlot();
        mondaySlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        mondaySlot.setStartTime("09:00");
        mondaySlot.setEndTime("17:00");

        tuesdaySlot = new CreateAvailabilityRequest.AvailabilitySlot();
        tuesdaySlot.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        tuesdaySlot.setStartTime("10:00");
        tuesdaySlot.setEndTime("18:00");

        wednesdaySlot = new CreateAvailabilityRequest.AvailabilitySlot();
        wednesdaySlot.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        wednesdaySlot.setStartTime("08:00");
        wednesdaySlot.setEndTime("16:00");
    }

    // ========== BATCH CREATE TESTS ==========

    @Test
    void createBatchAvailability_MultipleSlots_Success() {
        // Arrange
        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(mondaySlot, tuesdaySlot, wednesdaySlot));

        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 3 availability slots"));

        // Verify the service was called with the correct arguments
        verify(availabilityService, times(1)).createAvailability(eq(1L), argThat(list -> list != null &&
                list.size() == 3 &&
                list.get(0).getDayOfWeek() == Availability.DayOfWeek.MONDAY &&
                list.get(0).getStartTime().equals("09:00") &&
                list.get(0).getEndTime().equals("17:00") &&
                list.get(1).getDayOfWeek() == Availability.DayOfWeek.TUESDAY &&
                list.get(2).getDayOfWeek() == Availability.DayOfWeek.WEDNESDAY));
    }

    @Test
    void createBatchAvailability_MixedTimeFormats_Success() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot slot1 = new CreateAvailabilityRequest.AvailabilitySlot();
        slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot1.setStartTime("9:00"); // Without leading zero
        slot1.setEndTime("17:00");

        CreateAvailabilityRequest.AvailabilitySlot slot2 = new CreateAvailabilityRequest.AvailabilitySlot();
        slot2.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        slot2.setStartTime("09:00"); // With leading zero
        slot2.setEndTime("5:00 PM"); // 12-hour format (will be parsed by TimeUtils)

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(slot1, slot2));

        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(availabilityService, times(1)).createAvailability(eq(1L), argThat(list -> list.size() == 2));
    }

    @Test
    void createBatchAvailability_WithOverlappingSlotsInBatch_ThrowsError() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot overlappingSlot1 = new CreateAvailabilityRequest.AvailabilitySlot();
        overlappingSlot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        overlappingSlot1.setStartTime("09:00");
        overlappingSlot1.setEndTime("14:00");

        CreateAvailabilityRequest.AvailabilitySlot overlappingSlot2 = new CreateAvailabilityRequest.AvailabilitySlot();
        overlappingSlot2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        overlappingSlot2.setStartTime("13:00"); // Overlaps with slot1
        overlappingSlot2.setEndTime("17:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(overlappingSlot1, overlappingSlot2, wednesdaySlot));

        doThrow(new IllegalArgumentException("Overlap in new schedule: MONDAY 09:00-14:00 overlaps with 13:00-17:00"))
                .when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Overlap"));
    }

    @Test
    void createBatchAvailability_WithExistingOverlaps_ThrowsError() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot conflictingSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        conflictingSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        conflictingSlot.setStartTime("09:00");
        conflictingSlot.setEndTime("17:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(conflictingSlot, tuesdaySlot));

        doThrow(new IllegalArgumentException("Overlaps with existing MONDAY slot: 09:00 - 17:00"))
                .when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Overlaps with existing"));
    }

    @Test
    void createBatchAvailability_WithInvalidTimeInOneSlot_ReturnsErrorForEntireBatch() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot validSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        validSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        validSlot.setStartTime("09:00");
        validSlot.setEndTime("17:00");

        CreateAvailabilityRequest.AvailabilitySlot invalidSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        invalidSlot.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
        invalidSlot.setStartTime("25:00"); // Invalid hour
        invalidSlot.setEndTime("26:00"); // Invalid hour

        CreateAvailabilityRequest.AvailabilitySlot anotherValidSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        anotherValidSlot.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        anotherValidSlot.setStartTime("08:00");
        anotherValidSlot.setEndTime("16:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(validSlot, invalidSlot, anotherValidSlot));

        doThrow(new IllegalArgumentException(
                "Batch validation failed: Slot 2 (TUESDAY 25:00-26:00): Invalid time format"))
                .when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Batch validation failed"));
        assertTrue(response.getBody().getMessage().contains("Slot 2"));
    }

    @Test
    void createBatchAvailability_WithInvalidTimeOrder_ReturnsError() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot invalidSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        invalidSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        invalidSlot.setStartTime("17:00"); // Start after end
        invalidSlot.setEndTime("09:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(invalidSlot, tuesdaySlot));

        doThrow(new IllegalArgumentException(
                "Batch validation failed: Slot 1 (MONDAY 17:00-09:00): End time (09:00) must be after start time (17:00)"))
                .when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("must be after start time"));
    }

    @Test
    void createBatchAvailability_WithShortDuration_ReturnsError() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot shortSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        shortSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        shortSlot.setStartTime("09:00");
        shortSlot.setEndTime("09:15"); // Only 15 minutes

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(shortSlot));

        doThrow(new IllegalArgumentException(
                "Batch validation failed: Slot 1 (MONDAY 09:00-09:15): Minimum availability duration is 30 minutes"))
                .when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Minimum availability duration"));
    }

    @Test
    void createBatchAvailability_EmployeeNotFound_ReturnsError() {
        // Arrange
        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(999L);
        request.setAvailabilities(Arrays.asList(mondaySlot, tuesdaySlot));

        doThrow(new IllegalArgumentException("Employee not found with ID: 999"))
                .when(availabilityService).createAvailability(eq(999L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
    }

    @Test
    void createBatchAvailability_EmptyList_ClearsExistingAvailabilities() {
        // Arrange
        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Collections.emptyList());

        doNothing().when(availabilityService).createAvailability(eq(1L), eq(Collections.emptyList()));

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 0 availability slots"));
        verify(availabilityService, times(1)).createAvailability(eq(1L), eq(Collections.emptyList()));
    }

    @Test
    void createBatchAvailability_LargeBatch_Success() {
        // Arrange - Create a batch with slots for all 7 days
        List<CreateAvailabilityRequest.AvailabilitySlot> allWeekSlots = Arrays.asList(
                createSlot(Availability.DayOfWeek.MONDAY, "09:00", "17:00"),
                createSlot(Availability.DayOfWeek.TUESDAY, "09:00", "17:00"),
                createSlot(Availability.DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                createSlot(Availability.DayOfWeek.THURSDAY, "09:00", "17:00"),
                createSlot(Availability.DayOfWeek.FRIDAY, "09:00", "17:00"),
                createSlot(Availability.DayOfWeek.SATURDAY, "10:00", "14:00"),
                createSlot(Availability.DayOfWeek.SUNDAY, "10:00", "14:00"));

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(allWeekSlots);

        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 7 availability slots"));
        verify(availabilityService, times(1)).createAvailability(eq(1L), argThat(list -> list.size() == 7 &&
                list.stream().filter(s -> s.getDayOfWeek() == Availability.DayOfWeek.MONDAY).count() == 1 &&
                list.stream().filter(s -> s.getDayOfWeek() == Availability.DayOfWeek.SUNDAY).count() == 1));
    }

    @Test
    void createBatchAvailability_WithDifferentEmployeesInSeparateRequests_Success() {
        // Arrange - First employee
        CreateAvailabilityRequest request1 = new CreateAvailabilityRequest();
        request1.setEmployeeId(1L);
        request1.setAvailabilities(Arrays.asList(mondaySlot, tuesdaySlot));

        // Second employee
        CreateAvailabilityRequest.AvailabilitySlot employee2Slot = new CreateAvailabilityRequest.AvailabilitySlot();
        employee2Slot.setDayOfWeek(Availability.DayOfWeek.WEDNESDAY);
        employee2Slot.setStartTime("08:00");
        employee2Slot.setEndTime("16:00");

        CreateAvailabilityRequest request2 = new CreateAvailabilityRequest();
        request2.setEmployeeId(2L);
        request2.setAvailabilities(Arrays.asList(employee2Slot));

        // Mock service calls
        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());
        doNothing().when(availabilityService).createAvailability(eq(2L), anyList());

        // Act - Create for first employee
        ResponseEntity<ApiResponse<String>> response1 = availabilityController.createAvailability(request1);

        // Act - Create for second employee
        ResponseEntity<ApiResponse<String>> response2 = availabilityController.createAvailability(request2);

        // Assert
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertTrue(response1.getBody().isSuccess());

        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        assertTrue(response2.getBody().isSuccess());

        verify(availabilityService, times(1)).createAvailability(eq(1L), argThat(list -> list.size() == 2));
        verify(availabilityService, times(1)).createAvailability(eq(2L), argThat(list -> list.size() == 1));
    }

    @Test
    void createBatchAvailability_WithDuplicateDaysButNonOverlappingTimes_Success() {
        // Arrange - Multiple slots on same day but not overlapping
        CreateAvailabilityRequest.AvailabilitySlot morningSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        morningSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        morningSlot.setStartTime("09:00");
        morningSlot.setEndTime("12:00");

        CreateAvailabilityRequest.AvailabilitySlot afternoonSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        afternoonSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        afternoonSlot.setStartTime("13:00"); // Starts after morning slot ends
        afternoonSlot.setEndTime("17:00");

        CreateAvailabilityRequest.AvailabilitySlot eveningSlot = new CreateAvailabilityRequest.AvailabilitySlot();
        eveningSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        eveningSlot.setStartTime("18:00"); // Starts after afternoon slot ends
        eveningSlot.setEndTime("20:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(morningSlot, afternoonSlot, eveningSlot));

        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 3 availability slots"));
        verify(availabilityService, times(1)).createAvailability(eq(1L), argThat(list -> list.size() == 3 &&
                list.stream().allMatch(s -> s.getDayOfWeek() == Availability.DayOfWeek.MONDAY)));
    }

    @Test
    public void createBatchAvailability_WithNullSlotsList_CreatesEmptyList() {
        // Arrange
        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(null); // Null list

        // Mock service call with empty list (since controller converts null to empty
        // list)
        doNothing().when(availabilityService).createAvailability(eq(1L), eq(Collections.emptyList()));

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 0 availability slots"));
        verify(availabilityService, times(1)).createAvailability(eq(1L), eq(Collections.emptyList()));
    }

    @Test
    void createBatchAvailability_WithGapBetweenSlots_Success() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot slot1 = new CreateAvailabilityRequest.AvailabilitySlot();
        slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("12:00");

        CreateAvailabilityRequest.AvailabilitySlot slot2 = new CreateAvailabilityRequest.AvailabilitySlot();
        slot2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot2.setStartTime("12:30"); // 30-minute gap
        slot2.setEndTime("17:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(slot1, slot2));

        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(availabilityService, times(1)).createAvailability(eq(1L), argThat(list -> list.size() == 2));
    }

    @Test
    void createBatchAvailability_BackToBackSlots_Success() {
        // Arrange
        CreateAvailabilityRequest.AvailabilitySlot slot1 = new CreateAvailabilityRequest.AvailabilitySlot();
        slot1.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("12:00");

        CreateAvailabilityRequest.AvailabilitySlot slot2 = new CreateAvailabilityRequest.AvailabilitySlot();
        slot2.setDayOfWeek(Availability.DayOfWeek.MONDAY);
        slot2.setStartTime("12:00"); // Exactly when slot1 ends
        slot2.setEndTime("17:00");

        CreateAvailabilityRequest request = new CreateAvailabilityRequest();
        request.setEmployeeId(1L);
        request.setAvailabilities(Arrays.asList(slot1, slot2));

        doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        // Note: Depending on your overlap logic, back-to-back might be considered
        // overlapping or not
        // Your service's timesOverlap method uses isBefore() so back-to-back should be
        // allowed
        verify(availabilityService, times(1)).createAvailability(eq(1L), anyList());
    }

    // Helper method to create slots
    private CreateAvailabilityRequest.AvailabilitySlot createSlot(Availability.DayOfWeek day, String start,
            String end) {
        CreateAvailabilityRequest.AvailabilitySlot slot = new CreateAvailabilityRequest.AvailabilitySlot();
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return slot;
    }
}