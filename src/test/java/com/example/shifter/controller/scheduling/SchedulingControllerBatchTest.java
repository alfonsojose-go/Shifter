package com.example.shifter.controller.scheduling;

import com.example.shifter.dto.scheduling.*;
import com.example.shifter.dto.availability.*;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.service.scheduling.SchedulingService;
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
public class SchedulingControllerBatchTest {

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private SchedulingController schedulingController;

    private User testUser;
    private CreateSchedulingRequest.SchedulingSlot mondaySlot;
    private CreateSchedulingRequest.SchedulingSlot tuesdaySlot;
    private CreateSchedulingRequest.SchedulingSlot wednesdaySlot;

    @BeforeEach
    void setUp() {
        // Setup User
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");

        // Setup test slots
        mondaySlot = new CreateSchedulingRequest.SchedulingSlot();
        mondaySlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        mondaySlot.setStartTime("09:00");
        mondaySlot.setEndTime("17:00");

        tuesdaySlot = new CreateSchedulingRequest.SchedulingSlot();
        tuesdaySlot.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        tuesdaySlot.setStartTime("10:00");
        tuesdaySlot.setEndTime("18:00");

        wednesdaySlot = new CreateSchedulingRequest.SchedulingSlot();
        wednesdaySlot.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        wednesdaySlot.setStartTime("08:00");
        wednesdaySlot.setEndTime("16:00");
    }

    // ========== BATCH CREATE TESTS ==========

    @Test
    void createBatchScheduling_MultipleSlots_Success() {
        // Arrange
        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(mondaySlot, tuesdaySlot, wednesdaySlot));

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 3 scheduling slots"));

        // Verify the service was called with the correct arguments
        verify(schedulingService, times(1)).createSchedule(eq(1L), argThat(list -> list != null &&
                list.size() == 3 &&
                list.get(0).getDayOfWeek() == Scheduling.DayOfWeek.MONDAY &&
                list.get(0).getStartTime().equals("09:00") &&
                list.get(0).getEndTime().equals("17:00") &&
                list.get(1).getDayOfWeek() == Scheduling.DayOfWeek.TUESDAY &&
                list.get(2).getDayOfWeek() == Scheduling.DayOfWeek.WEDNESDAY));
    }

    @Test
    void createBatchScheduling_MixedTimeFormats_Success() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot slot1 = new CreateSchedulingRequest.SchedulingSlot();
        slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot1.setStartTime("9:00"); // Without leading zero
        slot1.setEndTime("17:00");

        CreateSchedulingRequest.SchedulingSlot slot2 = new CreateSchedulingRequest.SchedulingSlot();
        slot2.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        slot2.setStartTime("09:00"); // With leading zero
        slot2.setEndTime("5:00 PM"); // 12-hour format (will be parsed by TimeUtils)

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(slot1, slot2));

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(schedulingService, times(1)).createSchedule(eq(1L), argThat(list -> list.size() == 2));
    }

    @Test
    void createBatchScheduling_WithOverlappingSlotsInBatch_ThrowsError() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot overlappingSlot1 = new CreateSchedulingRequest.SchedulingSlot();
        overlappingSlot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        overlappingSlot1.setStartTime("09:00");
        overlappingSlot1.setEndTime("14:00");

        CreateSchedulingRequest.SchedulingSlot overlappingSlot2 = new CreateSchedulingRequest.SchedulingSlot();
        overlappingSlot2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        overlappingSlot2.setStartTime("13:00"); // Overlaps with slot1
        overlappingSlot2.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(overlappingSlot1, overlappingSlot2, wednesdaySlot));

        doThrow(new IllegalArgumentException("Overlap in new schedule: MONDAY 09:00-14:00 overlaps with 13:00-17:00"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Overlap"));
    }

    @Test
    void createBatchScheduling_WithExistingOverlaps_ThrowsError() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot conflictingSlot = new CreateSchedulingRequest.SchedulingSlot();
        conflictingSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        conflictingSlot.setStartTime("09:00");
        conflictingSlot.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(conflictingSlot, tuesdaySlot));

        doThrow(new IllegalArgumentException("Overlaps with existing MONDAY slot: 09:00 - 17:00"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Overlaps with existing"));
    }

    @Test
    void createBatchScheduling_WithInvalidTimeInOneSlot_ReturnsErrorForEntireBatch() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot validSlot = new CreateSchedulingRequest.SchedulingSlot();
        validSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        validSlot.setStartTime("09:00");
        validSlot.setEndTime("17:00");

        CreateSchedulingRequest.SchedulingSlot invalidSlot = new CreateSchedulingRequest.SchedulingSlot();
        invalidSlot.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        invalidSlot.setStartTime("25:00"); // Invalid hour
        invalidSlot.setEndTime("26:00"); // Invalid hour

        CreateSchedulingRequest.SchedulingSlot anotherValidSlot = new CreateSchedulingRequest.SchedulingSlot();
        anotherValidSlot.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        anotherValidSlot.setStartTime("08:00");
        anotherValidSlot.setEndTime("16:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(validSlot, invalidSlot, anotherValidSlot));

        doThrow(new IllegalArgumentException(
                "Batch validation failed: Slot 2 (TUESDAY 25:00-26:00): Invalid time format"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Batch validation failed"));
        assertTrue(response.getBody().getMessage().contains("Slot 2"));
    }

    @Test
    void createBatchScheduling_WithInvalidTimeOrder_ReturnsError() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot invalidSlot = new CreateSchedulingRequest.SchedulingSlot();
        invalidSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        invalidSlot.setStartTime("17:00"); // Start after end
        invalidSlot.setEndTime("09:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(invalidSlot, tuesdaySlot));

        doThrow(new IllegalArgumentException(
                "Batch validation failed: Slot 1 (MONDAY 17:00-09:00): End time (09:00) must be after start time (17:00)"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("must be after start time"));
    }

    @Test
    void createBatchScheduling_WithShortDuration_ReturnsError() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot shortSlot = new CreateSchedulingRequest.SchedulingSlot();
        shortSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        shortSlot.setStartTime("09:00");
        shortSlot.setEndTime("09:15"); // Only 15 minutes

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(shortSlot));

        doThrow(new IllegalArgumentException(
                "Batch validation failed: Slot 1 (MONDAY 09:00-09:15): Minimum scheduling duration is 30 minutes"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Minimum scheduling duration"));
    }

    @Test
    void createBatchScheduling_EmployeeNotFound_ReturnsError() {
        // Arrange
        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(999L);
        request.setScheduling(Arrays.asList(mondaySlot, tuesdaySlot));

        doThrow(new IllegalArgumentException("Employee not found with ID: 999"))
                .when(schedulingService).createSchedule(eq(999L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
    }

    @Test
    void createBatchScheduling_EmptyList_ClearsExistingScheduling() {
        // Arrange
        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Collections.emptyList());

        doNothing().when(schedulingService).createSchedule(eq(1L), eq(Collections.emptyList()));

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 0 scheduling slots"));
        verify(schedulingService, times(1)).createSchedule(eq(1L), eq(Collections.emptyList()));
    }

    @Test
    void createBatchScheduling_LargeBatch_Success() {
        // Arrange - Create a batch with slots for all 7 days
        List<CreateSchedulingRequest.SchedulingSlot> allWeekSlots = Arrays.asList(
                createSlot(Scheduling.DayOfWeek.MONDAY, "09:00", "17:00"),
                createSlot(Scheduling.DayOfWeek.TUESDAY, "09:00", "17:00"),
                createSlot(Scheduling.DayOfWeek.WEDNESDAY, "09:00", "17:00"),
                createSlot(Scheduling.DayOfWeek.THURSDAY, "09:00", "17:00"),
                createSlot(Scheduling.DayOfWeek.FRIDAY, "09:00", "17:00"),
                createSlot(Scheduling.DayOfWeek.SATURDAY, "10:00", "14:00"),
                createSlot(Scheduling.DayOfWeek.SUNDAY, "10:00", "14:00"));

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(allWeekSlots);

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 7 scheduling slots"));
        verify(schedulingService, times(1)).createSchedule(eq(1L), argThat(list -> list.size() == 7 &&
                list.stream().filter(s -> s.getDayOfWeek() == Scheduling.DayOfWeek.MONDAY).count() == 1 &&
                list.stream().filter(s -> s.getDayOfWeek() == Scheduling.DayOfWeek.SUNDAY).count() == 1));
    }

    @Test
    void createBatchScheduling_WithDifferentEmployeesInSeparateRequests_Success() {
        // Arrange - First employee
        CreateSchedulingRequest request1 = new CreateSchedulingRequest();
        request1.setEmployeeId(1L);
        request1.setScheduling(Arrays.asList(mondaySlot, tuesdaySlot));

        // Second employee
        CreateSchedulingRequest.SchedulingSlot employee2Slot = new CreateSchedulingRequest.SchedulingSlot();
        employee2Slot.setDayOfWeek(Scheduling.DayOfWeek.WEDNESDAY);
        employee2Slot.setStartTime("08:00");
        employee2Slot.setEndTime("16:00");

        CreateSchedulingRequest request2 = new CreateSchedulingRequest();
        request2.setEmployeeId(2L);
        request2.setScheduling(Arrays.asList(employee2Slot));

        // Mock service calls
        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());
        doNothing().when(schedulingService).createSchedule(eq(2L), anyList());

        // Act - Create for first employee
        ResponseEntity<ApiResponse<String>> response1 = schedulingController.createSchedule(request1);

        // Act - Create for second employee
        ResponseEntity<ApiResponse<String>> response2 = schedulingController.createSchedule(request2);

        // Assert
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertTrue(response1.getBody().isSuccess());

        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        assertTrue(response2.getBody().isSuccess());

        verify(schedulingService, times(1)).createSchedule(eq(1L), argThat(list -> list.size() == 2));
        verify(schedulingService, times(1)).createSchedule(eq(2L), argThat(list -> list.size() == 1));
    }

    @Test
    void createBatchScheduling_WithDuplicateDaysButNonOverlappingTimes_Success() {
        // Arrange - Multiple slots on same day but not overlapping
        CreateSchedulingRequest.SchedulingSlot morningSlot = new CreateSchedulingRequest.SchedulingSlot();
        morningSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        morningSlot.setStartTime("09:00");
        morningSlot.setEndTime("12:00");

        CreateSchedulingRequest.SchedulingSlot afternoonSlot = new CreateSchedulingRequest.SchedulingSlot();
        afternoonSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        afternoonSlot.setStartTime("13:00"); // Starts after morning slot ends
        afternoonSlot.setEndTime("17:00");

        CreateSchedulingRequest.SchedulingSlot eveningSlot = new CreateSchedulingRequest.SchedulingSlot();
        eveningSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        eveningSlot.setStartTime("18:00"); // Starts after afternoon slot ends
        eveningSlot.setEndTime("20:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(morningSlot, afternoonSlot, eveningSlot));

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 3 scheduling slots"));
        verify(schedulingService, times(1)).createSchedule(eq(1L), argThat(list -> list.size() == 3 &&
                list.stream().allMatch(s -> s.getDayOfWeek() == Scheduling.DayOfWeek.MONDAY)));
    }

    @Test
    public void createBatchScheduling_WithNullSlotsList_CreatesEmptyList() {
        // Arrange
        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(null); // Null list

        // Mock service call with empty list (since controller converts null to empty
        // list)
        doNothing().when(schedulingService).createSchedule(eq(1L), eq(Collections.emptyList()));

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created 0 scheduling slots"));
        verify(schedulingService, times(1)).createSchedule(eq(1L), eq(Collections.emptyList()));
    }

    @Test
    void createBatchScheduling_WithGapBetweenSlots_Success() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot slot1 = new CreateSchedulingRequest.SchedulingSlot();
        slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("12:00");

        CreateSchedulingRequest.SchedulingSlot slot2 = new CreateSchedulingRequest.SchedulingSlot();
        slot2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot2.setStartTime("12:30"); // 30-minute gap
        slot2.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(slot1, slot2));

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(schedulingService, times(1)).createSchedule(eq(1L), argThat(list -> list.size() == 2));
    }

    @Test
    void createBatchScheduling_BackToBackSlots_Success() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot slot1 = new CreateSchedulingRequest.SchedulingSlot();
        slot1.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot1.setStartTime("09:00");
        slot1.setEndTime("12:00");

        CreateSchedulingRequest.SchedulingSlot slot2 = new CreateSchedulingRequest.SchedulingSlot();
        slot2.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        slot2.setStartTime("12:00"); // Exactly when slot1 ends
        slot2.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Arrays.asList(slot1, slot2));

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        // Note: Depending on your overlap logic, back-to-back might be considered
        // overlapping or not
        // Your service's timesOverlap method uses isBefore() so back-to-back should be
        // allowed
        verify(schedulingService, times(1)).createSchedule(eq(1L), anyList());
    }

    // Helper method to create slots
    private CreateSchedulingRequest.SchedulingSlot createSlot(Scheduling.DayOfWeek day, String start,
            String end) {
        CreateSchedulingRequest.SchedulingSlot slot = new CreateSchedulingRequest.SchedulingSlot();
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return slot;
    }
}