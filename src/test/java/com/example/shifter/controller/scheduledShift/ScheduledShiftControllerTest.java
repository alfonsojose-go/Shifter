package com.example.shifter.controller.scheduledShift;

import com.example.shifter.dto.availability.ApiResponse;
import com.example.shifter.dto.scheduledShift.ScheduledShiftRequest;
import com.example.shifter.dto.scheduledShift.ScheduledShiftResponse;
import com.example.shifter.service.position_scheduling.ScheduledShiftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledShiftControllerTest {

    @Mock
    private ScheduledShiftService scheduledShiftService;

    @InjectMocks
    private ScheduledShiftController scheduledShiftController;

    private ScheduledShiftRequest validRequest;
    private ScheduledShiftResponse expectedResponse;
    private ScheduledShiftResponse expectedResponse2;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        LocalTime nineAM = LocalTime.of(9, 0);
        LocalTime fivePM = LocalTime.of(17, 0);
        LocalTime twoPM = LocalTime.of(14, 0);
        LocalTime tenPM = LocalTime.of(22, 0);
        
        // Using constructor with setters (based on your existing test pattern)
        validRequest = new ScheduledShiftRequest();
        validRequest.setEmployeeId(1001L);
        validRequest.setDate(today.plusDays(1));
        validRequest.setStartTime(nineAM);
        validRequest.setEndTime(fivePM);

        expectedResponse = new ScheduledShiftResponse();
        expectedResponse.setId(1L);
        expectedResponse.setEmployeeId(1001L);
        expectedResponse.setEmployeeName("John Doe");
        expectedResponse.setPositionName("Cashier");
        expectedResponse.setHourlyWage(new BigDecimal("18.50"));
        expectedResponse.setDate(today.plusDays(1));
        expectedResponse.setDayOfWeek(today.plusDays(1).getDayOfWeek());
        expectedResponse.setStartTime(nineAM);
        expectedResponse.setEndTime(fivePM);
        expectedResponse.setCreatedAt(LocalDateTime.now());

        expectedResponse2 = new ScheduledShiftResponse();
        expectedResponse2.setId(2L);
        expectedResponse2.setEmployeeId(1002L);
        expectedResponse2.setEmployeeName("Jane Smith");
        expectedResponse2.setPositionName("Barista");
        expectedResponse2.setHourlyWage(new BigDecimal("17.00"));
        expectedResponse2.setDate(today.plusDays(2));
        expectedResponse2.setDayOfWeek(today.plusDays(2).getDayOfWeek());
        expectedResponse2.setStartTime(twoPM);
        expectedResponse2.setEndTime(tenPM);
        expectedResponse2.setCreatedAt(LocalDateTime.now());
    }

    // ========== CREATE SCHEDULED SHIFT TESTS ==========

    @Test
    void createScheduledShift_Success() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
        assertEquals(1L, response.getBody().getData().getId());
        assertEquals(1001L, response.getBody().getData().getEmployeeId());
        assertEquals("John Doe", response.getBody().getData().getEmployeeName());
        assertEquals("Cashier", response.getBody().getData().getPositionName());
        assertEquals(new BigDecimal("18.50"), response.getBody().getData().getHourlyWage());
        assertEquals(validRequest.getDate(), response.getBody().getData().getDate());
        assertEquals(validRequest.getStartTime(), response.getBody().getData().getStartTime());
        assertEquals(validRequest.getEndTime(), response.getBody().getData().getEndTime());
        assertEquals("Scheduled shift created successfully", response.getBody().getMessage());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    @Test
    void createScheduledShift_InvalidInput_ReturnsBadRequest() {
        // Arrange
        String errorMessage = "Invalid employee ID: Employee not found";
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertNull(response.getBody().getData());
        assertEquals(errorMessage, response.getBody().getMessage());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    @Test
    void createScheduledShift_NullEmployeeId_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(null);
        invalidRequest.setDate(LocalDate.now().plusDays(1));
        invalidRequest.setStartTime(LocalTime.of(9, 0));
        invalidRequest.setEndTime(LocalTime.of(17, 0));
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Employee ID cannot be null"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee ID cannot be null", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_NegativeEmployeeId_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(-1L);
        invalidRequest.setDate(LocalDate.now().plusDays(1));
        invalidRequest.setStartTime(LocalTime.of(9, 0));
        invalidRequest.setEndTime(LocalTime.of(17, 0));
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Employee ID cannot be negative"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee ID cannot be negative", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_NullDate_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(1001L);
        invalidRequest.setDate(null);
        invalidRequest.setStartTime(LocalTime.of(9, 0));
        invalidRequest.setEndTime(LocalTime.of(17, 0));
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Date cannot be null"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Date cannot be null", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_PastDate_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(1001L);
        invalidRequest.setDate(LocalDate.now().minusDays(1)); // Past date
        invalidRequest.setStartTime(LocalTime.of(9, 0));
        invalidRequest.setEndTime(LocalTime.of(17, 0));
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Shift date cannot be in the past"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Shift date cannot be in the past", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_NullStartTime_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(1001L);
        invalidRequest.setDate(LocalDate.now().plusDays(1));
        invalidRequest.setStartTime(null);
        invalidRequest.setEndTime(LocalTime.of(17, 0));
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Start time cannot be null"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Start time cannot be null", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_NullEndTime_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(1001L);
        invalidRequest.setDate(LocalDate.now().plusDays(1));
        invalidRequest.setStartTime(LocalTime.of(9, 0));
        invalidRequest.setEndTime(null);
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("End time cannot be null"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("End time cannot be null", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_InvalidTimeRange_ReturnsBadRequest() {
        // Arrange
        ScheduledShiftRequest invalidRequest = new ScheduledShiftRequest();
        invalidRequest.setEmployeeId(1001L);
        invalidRequest.setDate(LocalDate.now().plusDays(1));
        invalidRequest.setStartTime(LocalTime.of(17, 0));
        invalidRequest.setEndTime(LocalTime.of(9, 0)); // End time before start time
        
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("End time must be after start time"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(invalidRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("End time must be after start time", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(invalidRequest);
    }

    @Test
    void createScheduledShift_PositionNotFound_ReturnsBadRequest() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Position not found with ID: 9999"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Position not found with ID: 9999", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    @Test
    void createScheduledShift_EmployeeNotAvailable_ReturnsBadRequest() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Employee is not available on this date"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee is not available on this date", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    @Test
    void createScheduledShift_ShiftOverlap_ReturnsBadRequest() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Shift overlaps with existing schedule"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Shift overlaps with existing schedule", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    @Test
    void createScheduledShift_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to create scheduled shift", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    // ========== EXCEPTION MAPPING TESTS ==========

    @Test
    void createScheduledShift_WhenServiceThrowsDifferentExceptions_MapsToCorrectHttpStatus() {
        // Test IllegalArgumentException -> BAD_REQUEST
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid input"));
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response1 =
                scheduledShiftController.createScheduledShift(validRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        assertEquals("Invalid input", response1.getBody().getMessage());
        assertNull(response1.getBody().getData());
        
        // Test RuntimeException -> INTERNAL_SERVER_ERROR
        reset(scheduledShiftService);
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new RuntimeException("Database error"));
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response2 =
                scheduledShiftController.createScheduledShift(validRequest);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response2.getStatusCode());
        assertEquals("Failed to create scheduled shift", response2.getBody().getMessage());
        assertNull(response2.getBody().getData());
        
        // Test NullPointerException -> INTERNAL_SERVER_ERROR
        reset(scheduledShiftService);
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new NullPointerException("Null reference"));
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response3 =
                scheduledShiftController.createScheduledShift(validRequest);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response3.getStatusCode());
        assertEquals("Failed to create scheduled shift", response3.getBody().getMessage());
        assertNull(response3.getBody().getData());
    }

    // ========== API RESPONSE STRUCTURE TESTS ==========

    @Test
    void createScheduledShift_VerifyApiResponseStructure() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        ApiResponse<ScheduledShiftResponse> apiResponse = response.getBody();

        assertAll("Verify API response structure",
            () -> assertNotNull(apiResponse),
            () -> assertTrue(apiResponse.isSuccess()),
            () -> assertNotNull(apiResponse.getData()),
            () -> assertEquals(1L, apiResponse.getData().getId()),
            () -> assertEquals("Scheduled shift created successfully", apiResponse.getMessage())
        );
    }

    @Test
    void createScheduledShift_ErrorResponse_IncludesErrorMessage() {
        // Arrange
        String errorMessage = "Employee not found with ID: 999";
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        ApiResponse<ScheduledShiftResponse> apiResponse = response.getBody();
        
        assertAll("Verify error response structure",
            () -> assertNotNull(apiResponse),
            () -> assertFalse(apiResponse.isSuccess()),
            () -> assertNull(apiResponse.getData()),
            () -> assertEquals(errorMessage, apiResponse.getMessage())
        );
    }

    // ========== DTO FIELD INTEGRITY TESTS ==========

    @Test
    void createScheduledShift_VerifyAllFieldsArePopulated() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        ScheduledShiftResponse dto = response.getBody().getData();
        LocalDate expectedDate = LocalDate.now().plusDays(1);
        DayOfWeek expectedDayOfWeek = expectedDate.getDayOfWeek();
        
        assertAll("Verify all DTO fields are correctly populated",
            () -> assertEquals(1L, dto.getId()),
            () -> assertEquals(1001L, dto.getEmployeeId()),
            () -> assertEquals("John Doe", dto.getEmployeeName()),
            () -> assertEquals("Cashier", dto.getPositionName()),
            () -> assertEquals(new BigDecimal("18.50"), dto.getHourlyWage()),
            () -> assertEquals(expectedDate, dto.getDate()),
            () -> assertEquals(expectedDayOfWeek, dto.getDayOfWeek()),
            () -> assertEquals(LocalTime.of(9, 0), dto.getStartTime()),
            () -> assertEquals(LocalTime.of(17, 0), dto.getEndTime()),
            () -> assertNotNull(dto.getCreatedAt())
        );
    }

    @Test
    void createScheduledShift_UnassignedPosition_ReturnsCorrectly() {
        // Arrange - employee with no position (service defaults to "Unassigned" / ZERO wage)
        LocalDate shiftDate = LocalDate.now().plusDays(1);
        
        ScheduledShiftResponse unassignedResponse = new ScheduledShiftResponse();
        unassignedResponse.setId(3L);
        unassignedResponse.setEmployeeId(1003L);
        unassignedResponse.setEmployeeName("Bob Unassigned");
        unassignedResponse.setPositionName("Unassigned");
        unassignedResponse.setHourlyWage(BigDecimal.ZERO);
        unassignedResponse.setDate(shiftDate);
        unassignedResponse.setDayOfWeek(shiftDate.getDayOfWeek());
        unassignedResponse.setStartTime(LocalTime.of(10, 0));
        unassignedResponse.setEndTime(LocalTime.of(18, 0));
        unassignedResponse.setCreatedAt(LocalDateTime.now());

        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(unassignedResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Unassigned", response.getBody().getData().getPositionName());
        assertEquals(BigDecimal.ZERO, response.getBody().getData().getHourlyWage());
    }

    @Test
    void createScheduledShift_WithNoHourlyWage_ReturnsCorrectly() {
        // Arrange
        LocalDate shiftDate = LocalDate.now().plusDays(1);
        
        ScheduledShiftResponse noWageResponse = new ScheduledShiftResponse();
        noWageResponse.setId(4L);
        noWageResponse.setEmployeeId(1004L);
        noWageResponse.setEmployeeName("Test Employee");
        noWageResponse.setPositionName("Volunteer");
        noWageResponse.setHourlyWage(null); // No wage
        noWageResponse.setDate(shiftDate);
        noWageResponse.setDayOfWeek(shiftDate.getDayOfWeek());
        noWageResponse.setStartTime(LocalTime.of(9, 0));
        noWageResponse.setEndTime(LocalTime.of(17, 0));
        noWageResponse.setCreatedAt(LocalDateTime.now());

        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(noWageResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody().getData().getHourlyWage());
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    void createScheduledShift_ShouldCompleteWithinReasonableTime() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                    scheduledShiftController.createScheduledShift(validRequest);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody().getData());
        });
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void createScheduledShift_ZeroEmployeeId_ReturnsSuccess() {
        // Arrange - Some systems might use 0 as valid ID
        ScheduledShiftRequest zeroIdRequest = new ScheduledShiftRequest();
        zeroIdRequest.setEmployeeId(0L);
        zeroIdRequest.setDate(LocalDate.now().plusDays(1));
        zeroIdRequest.setStartTime(LocalTime.of(9, 0));
        zeroIdRequest.setEndTime(LocalTime.of(17, 0));

        LocalDate shiftDate = LocalDate.now().plusDays(1);
        
        ScheduledShiftResponse zeroIdResponse = new ScheduledShiftResponse();
        zeroIdResponse.setId(1L);
        zeroIdResponse.setEmployeeId(0L);
        zeroIdResponse.setEmployeeName("Zero ID Employee");
        zeroIdResponse.setPositionName("Cashier");
        zeroIdResponse.setHourlyWage(new BigDecimal("18.50"));
        zeroIdResponse.setDate(shiftDate);
        zeroIdResponse.setDayOfWeek(shiftDate.getDayOfWeek());
        zeroIdResponse.setStartTime(LocalTime.of(9, 0));
        zeroIdResponse.setEndTime(LocalTime.of(17, 0));
        zeroIdResponse.setCreatedAt(LocalDateTime.now());

        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(zeroIdResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(zeroIdRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(0L, response.getBody().getData().getEmployeeId());
        verify(scheduledShiftService, times(1)).createScheduledShift(zeroIdRequest);
    }

    @Test
    void createScheduledShift_MidnightShift_HandlesCorrectly() {
        // Arrange
        ScheduledShiftRequest midnightRequest = new ScheduledShiftRequest();
        midnightRequest.setEmployeeId(1001L);
        midnightRequest.setDate(LocalDate.now().plusDays(1));
        midnightRequest.setStartTime(LocalTime.of(0, 0));
        midnightRequest.setEndTime(LocalTime.of(8, 0));

        LocalDate shiftDate = LocalDate.now().plusDays(1);
        
        ScheduledShiftResponse midnightResponse = new ScheduledShiftResponse();
        midnightResponse.setId(1L);
        midnightResponse.setEmployeeId(1001L);
        midnightResponse.setEmployeeName("Night Worker");
        midnightResponse.setPositionName("Security");
        midnightResponse.setHourlyWage(new BigDecimal("20.00"));
        midnightResponse.setDate(shiftDate);
        midnightResponse.setDayOfWeek(shiftDate.getDayOfWeek());
        midnightResponse.setStartTime(LocalTime.of(0, 0));
        midnightResponse.setEndTime(LocalTime.of(8, 0));
        midnightResponse.setCreatedAt(LocalDateTime.now());

        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(midnightResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(midnightRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(LocalTime.of(0, 0), response.getBody().getData().getStartTime());
        assertEquals(LocalTime.of(8, 0), response.getBody().getData().getEndTime());
        verify(scheduledShiftService, times(1)).createScheduledShift(midnightRequest);
    }

    @Test
    void createScheduledShift_ServiceReturnsNull_HandlesGracefully() {
        // Arrange
        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(null);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(validRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertNull(response.getBody().getData());
        assertEquals("Scheduled shift created successfully", response.getBody().getMessage());
        verify(scheduledShiftService, times(1)).createScheduledShift(validRequest);
    }

    @Test
    void createScheduledShift_FarFutureDate_HandlesCorrectly() {
        // Arrange
        ScheduledShiftRequest futureRequest = new ScheduledShiftRequest();
        futureRequest.setEmployeeId(1001L);
        futureRequest.setDate(LocalDate.now().plusYears(1));
        futureRequest.setStartTime(LocalTime.of(9, 0));
        futureRequest.setEndTime(LocalTime.of(17, 0));

        LocalDate futureDate = LocalDate.now().plusYears(1);
        
        ScheduledShiftResponse futureResponse = new ScheduledShiftResponse();
        futureResponse.setId(1L);
        futureResponse.setEmployeeId(1001L);
        futureResponse.setEmployeeName("John Doe");
        futureResponse.setPositionName("Cashier");
        futureResponse.setHourlyWage(new BigDecimal("18.50"));
        futureResponse.setDate(futureDate);
        futureResponse.setDayOfWeek(futureDate.getDayOfWeek());
        futureResponse.setStartTime(LocalTime.of(9, 0));
        futureResponse.setEndTime(LocalTime.of(17, 0));
        futureResponse.setCreatedAt(LocalDateTime.now());

        when(scheduledShiftService.createScheduledShift(any(ScheduledShiftRequest.class)))
                .thenReturn(futureResponse);

        // Act
        ResponseEntity<ApiResponse<ScheduledShiftResponse>> response = 
                scheduledShiftController.createScheduledShift(futureRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(futureDate, response.getBody().getData().getDate());
        assertEquals(futureDate.getDayOfWeek(), response.getBody().getData().getDayOfWeek());
        verify(scheduledShiftService, times(1)).createScheduledShift(futureRequest);
    }
}