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

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerTest {

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private SchedulingController schedulingController;

    private User testUser;
    private Scheduling testScheduling;
    private SchedulingResponse testSchedulingResponse;
    private EmployeeResponse testEmployeeResponse;


    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");

        testScheduling = new Scheduling();
        testScheduling.setSchedulingId(1L);
        testScheduling.setEmployee(testUser);
        testScheduling.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        testScheduling.setStartTime(LocalTime.of(9, 0));
        testScheduling.setEndTime(LocalTime.of(17, 0));

        testSchedulingResponse = new SchedulingResponse();
        testSchedulingResponse.setSchedulingId(1L);
        testSchedulingResponse.setEmployeeId(1L);
        testSchedulingResponse.setEmployeeName("John Doe");
        testSchedulingResponse.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        testSchedulingResponse.setStartTime("09:00 AM");
        testSchedulingResponse.setEndTime("05:00 PM");

        testEmployeeResponse = new EmployeeResponse();
        testEmployeeResponse.setId(1L);
        testEmployeeResponse.setFullName("John Doe");
        testEmployeeResponse.setUsername("johndoe");
        testEmployeeResponse.setEmail("john@example.com");
    }

    // ========== CREATE SCHEDULING TESTS ==========
    @Test
    void createScheduling_Success() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot requestSlot = new CreateSchedulingRequest.SchedulingSlot();
        requestSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        requestSlot.setStartTime("09:00");
        requestSlot.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Collections.singletonList(requestSlot));

        doNothing().when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Successfully created"));
        verify(schedulingService, times(1)).createSchedule(eq(1L), anyList());
    }

    @Test
    void createScheduling_WithEmptySlots_ClearsScheduling() {
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
        verify(schedulingService, times(1)).createSchedule(eq(1L), eq(Collections.emptyList()));
    }

    @Test
    void createSchedule_InvalidInput_ReturnsBadRequest() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot requestSlot = new CreateSchedulingRequest.SchedulingSlot();
        requestSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        requestSlot.setStartTime("invalid-time");
        requestSlot.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Collections.singletonList(requestSlot));

        doThrow(new IllegalArgumentException("Invalid time format"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid time format", response.getBody().getMessage());
    }

    @Test
    void createScheduling_OverlapError_ReturnsBadRequest() {
        // Arrange
        CreateSchedulingRequest.SchedulingSlot requestSlot = new CreateSchedulingRequest.SchedulingSlot();
        requestSlot.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        requestSlot.setStartTime("09:00");
        requestSlot.setEndTime("17:00");

        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Collections.singletonList(requestSlot));

        doThrow(new IllegalArgumentException("Overlaps with existing MONDAY slot: 09:00 AM - 05:00 PM"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Overlaps"));
    }

    // ========== GET ALL SCHEDULING TESTS ==========
    @Test
    void getAllSchedules_Success() {
        // Arrange
        List<Scheduling> scheduling = Arrays.asList(testScheduling);
        when(schedulingService.getAllSchedules()).thenReturn(scheduling);

        // Act
        ResponseEntity<ApiResponse<List<SchedulingResponse>>> response = schedulingController
                .getAllSchedules();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Schedules retrieved successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        assertEquals(1, response.getBody().getData().size());
        verify(schedulingService, times(1)).getAllSchedules();
    }

    @Test
    void getAllSchedules_EmptyList() {
        // Arrange
        when(schedulingService.getAllSchedules()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ApiResponse<List<SchedulingResponse>>> response = schedulingController
                .getAllSchedules();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().isEmpty());
    }

    // ========== GET SCHEDULING BY ID TESTS ==========
    @Test
    void getScheduleById_Success() {
        // Arrange
        when(schedulingService.getScheduleById(1L)).thenReturn(testScheduling);

        // Act
        ResponseEntity<ApiResponse<SchedulingResponse>> response = schedulingController
                .getScheduleById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Scheduling retrieved successfully", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        verify(schedulingService, times(1)).getScheduleById(1L);
    }

    @Test
    void getScheduleById_NotFound() {
        // Arrange
        when(schedulingService.getScheduleById(999L))
                .thenThrow(new IllegalArgumentException("Scheduling not found with ID: 999"));

        // Act
        ResponseEntity<ApiResponse<SchedulingResponse>> response = schedulingController
                .getScheduleById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Scheduling not found with ID: 999", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    // ========== GET SCHEDULING BY EMPLOYEE TESTS ==========
    @Test
    void getScheduleByEmployee_Success() {
        // Arrange
        List<Scheduling> scheduling = Arrays.asList(testScheduling);
        when(schedulingService.getSchedulesByEmployee(1L)).thenReturn(scheduling);

        // Act
        ResponseEntity<ApiResponse<List<SchedulingResponse>>> response = schedulingController
                .getSchedulesByEmployee(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Employee schedules retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(schedulingService, times(1)).getSchedulesByEmployee(1L);
    }

    @Test
    void getSchedulesByEmployee_EmployeeNotFound() {
        // Arrange
        when(schedulingService.getSchedulesByEmployee(999L))
                .thenThrow(new IllegalArgumentException("Employee not found with ID: 999"));

        // Act
        ResponseEntity<ApiResponse<List<SchedulingResponse>>> response = schedulingController
                .getSchedulesByEmployee(999L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
    }

    // ========== GET SCHEDULING BY EMPLOYEE AND DAY TESTS ==========
    @Test
    void getSchedulesByEmployeeAndDay_Success() {
        // Arrange
        List<Scheduling> scheduling = Arrays.asList(testScheduling);
        when(schedulingService.getSchedulesByEmployeeAndDay(1L, Scheduling.DayOfWeek.MONDAY))
                .thenReturn(scheduling);

        // Act
        ResponseEntity<ApiResponse<List<SchedulingResponse>>> response = schedulingController
                .getSchedulesByEmployeeAndDay(1L, Scheduling.DayOfWeek.MONDAY);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Employee day schedules retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(schedulingService, times(1)).getSchedulesByEmployeeAndDay(1L,
                Scheduling.DayOfWeek.MONDAY);
    }

    // ========== UPDATE SCHEDULING TESTS ==========
    @Test
    void updateScheduling_Success() {
        // Arrange
        UpdateSchedulingRequest request = new UpdateSchedulingRequest();
        request.setDayOfWeek(Scheduling.DayOfWeek.TUESDAY);
        request.setStartTime("10:00");
        request.setEndTime("18:00");

        doNothing().when(schedulingService).updateScheduleById(
                eq(1L), eq(Scheduling.DayOfWeek.TUESDAY), eq("10:00"), eq("18:00"));

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.updateScheduling(1L, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Scheduling updated successfully", response.getBody().getMessage());
        verify(schedulingService, times(1)).updateScheduleById(1L,
                Scheduling.DayOfWeek.TUESDAY, "10:00", "18:00");
    }

    @Test
    void updateScheduling_OverlapError() {
        // Arrange
        UpdateSchedulingRequest request = new UpdateSchedulingRequest();
        request.setDayOfWeek(Scheduling.DayOfWeek.MONDAY);
        request.setStartTime("09:00");
        request.setEndTime("17:00");

        doThrow(new IllegalArgumentException("Overlaps with existing MONDAY slot: 09:00 AM - 05:00 PM"))
                .when(schedulingService)
                .updateScheduleById(eq(1L), any(), anyString(), anyString());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.updateScheduling(1L, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Overlaps"));
    }

    // ========== DELETE SCHEDULING TESTS ==========
    @Test
    void deleteScheduling_Success() {
        // Arrange
        doNothing().when(schedulingService).deleteScheduleById(1L);

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.deleteScheduling(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Scheduling deleted successfully", response.getBody().getMessage());
        verify(schedulingService, times(1)).deleteScheduleById(1L);
    }

    @Test
    void deleteScheduling_NotFound() {
        // Arrange
        doThrow(new IllegalArgumentException("Scheduling not found with ID: 999"))
                .when(schedulingService).deleteScheduleById(999L);

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.deleteScheduling(999L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Scheduling not found with ID: 999", response.getBody().getMessage());
    }

    // ========== DELETE ALL SCHEDULING OF EMPLOYEE TESTS ==========
    @Test
    void deleteAllScheduleOfEmployee_Success() {
        // Arrange
        doNothing().when(schedulingService).deleteAllScheduleOfEmployee(1L);

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController
                .deleteAllScheduleOfEmployee(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("All schedules for employee deleted successfully", response.getBody().getMessage());
        verify(schedulingService, times(1)).deleteAllScheduleOfEmployee(1L);
    }

    @Test
    void deleteAllScheduleOfEmployee_EmployeeNotFound() {
        // Arrange
        doThrow(new IllegalArgumentException("Employee not found with ID: 999"))
                .when(schedulingService).deleteAllScheduleOfEmployee(999L);

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController
                .deleteAllScheduleOfEmployee(999L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
    }

    // ========== VALIDATE TIME RANGE TESTS ==========
    @Test
    void validateTimeRange_Valid() {
        // Arrange
        doNothing().when(schedulingService).validateTimeRange("09:00", "17:00");

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.validateTimeRange("09:00",
                "17:00");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Time range is valid", response.getBody().getMessage());
        verify(schedulingService, times(1)).validateTimeRange("09:00", "17:00");
    }

    @Test
    void validateTimeRange_Invalid() {
        // Arrange
        doThrow(new IllegalArgumentException("End time must be after start time"))
                .when(schedulingService).validateTimeRange("17:00", "09:00");

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.validateTimeRange("17:00",
                "09:00");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("End time must be after start time", response.getBody().getMessage());
    }

    // ========== GET AVAILABLE EMPLOYEES BY DAY TESTS ==========
    @Test
    void getAvailableEmployeesByDay_Success() {
        // Arrange
        List<User> employees = Arrays.asList(testUser);
        when(schedulingService.getAvailableEmployeesByDay(Scheduling.DayOfWeek.MONDAY))
                .thenReturn(employees);

        // Act
        ResponseEntity<ApiResponse<List<EmployeeResponse>>> response = schedulingController
                .getAvailableEmployeesByDay(Scheduling.DayOfWeek.MONDAY);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Available employees retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("John Doe", response.getBody().getData().get(0).getFullName());
        verify(schedulingService, times(1)).getAvailableEmployeesByDay(Scheduling.DayOfWeek.MONDAY);
    }

    @Test
    void getAvailableEmployeesByDay_EmptyList() {
        // Arrange
        when(schedulingService.getAvailableEmployeesByDay(Scheduling.DayOfWeek.SUNDAY))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ApiResponse<List<EmployeeResponse>>> response = schedulingController
                .getAvailableEmployeesByDay(Scheduling.DayOfWeek.SUNDAY);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().isEmpty());
    }

    // ========== HAS SCHEDULING TESTS ==========
    @Test
    void hasSchedule_True() {
        // Arrange
        when(schedulingService.hasSchedule(1L)).thenReturn(true);

        // Act
        ResponseEntity<ApiResponse<Boolean>> response = schedulingController.hasSchedule(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Scheduling check completed successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData());
        verify(schedulingService, times(1)).hasSchedule(1L);
    }

    @Test
    void hasSchedule_False() {
        // Arrange
        when(schedulingService.hasSchedule(2L)).thenReturn(false);

        // Act
        ResponseEntity<ApiResponse<Boolean>> response = schedulingController.hasSchedule(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertFalse(response.getBody().getData());
        verify(schedulingService, times(1)).hasSchedule(2L);
    }

    // ========== EXCEPTION HANDLING TESTS ==========
    @Test
    void createScheduling_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        CreateSchedulingRequest request = new CreateSchedulingRequest();
        request.setEmployeeId(1L);
        request.setScheduling(Collections.emptyList());

        doThrow(new RuntimeException("Database connection failed"))
                .when(schedulingService).createSchedule(eq(1L), anyList());

        // Act
        ResponseEntity<ApiResponse<String>> response = schedulingController.createSchedule(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void getAllSchedules_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        when(schedulingService.getAllSchedules())
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<SchedulingResponse>>> response = schedulingController
                .getAllSchedules();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to retrieve schedules", response.getBody().getMessage());
    }
}