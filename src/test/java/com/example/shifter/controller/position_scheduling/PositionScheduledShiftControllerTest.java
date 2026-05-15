package com.example.shifter.controller.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeShiftDTO;
import com.example.shifter.dto.availability.ApiResponse;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@ExtendWith(MockitoExtension.class)
class PositionScheduledShiftControllerTest {

    @Mock
    private ScheduledShiftService scheduledShiftService;

    @InjectMocks
    private PositionScheduledShiftController scheduledShiftController;

    private EmployeeShiftDTO testShiftDTO;
    private EmployeeShiftDTO testShiftDTO2;

    @BeforeEach
    void setUp() {
        testShiftDTO = new EmployeeShiftDTO(
                1L,
                "John Doe",
                "Cashier",
                new BigDecimal("18.50"),
                1L,
                LocalDate.of(2025, 6, 2),   // Monday
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        testShiftDTO2 = new EmployeeShiftDTO(
                2L,
                "Jane Smith",
                "Barista",
                new BigDecimal("17.00"),
                2L,
                LocalDate.of(2025, 6, 3),   // Tuesday
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0)
        );
    }

    // ========== GET ALL SCHEDULES TESTS ==========

    @Test
    void getAllSchedules_Success() {
        // Arrange
        List<EmployeeShiftDTO> shifts = Arrays.asList(testShiftDTO, testShiftDTO2);
        when(scheduledShiftService.getAllShiftsWithPositions()).thenReturn(shifts);

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Schedules retrieved successfully", response.getBody().getMessage());
        assertEquals(2, response.getBody().getData().size());
        assertEquals("John Doe", response.getBody().getData().get(0).getFullName());
        assertEquals("Cashier", response.getBody().getData().get(0).getPositionName());
        verify(scheduledShiftService, times(1)).getAllShiftsWithPositions();
    }

    @Test
    void getAllSchedules_EmptyList() {
        // Arrange
        when(scheduledShiftService.getAllShiftsWithPositions()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Schedules retrieved successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(scheduledShiftService, times(1)).getAllShiftsWithPositions();
    }

    @Test
    void getAllSchedules_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        when(scheduledShiftService.getAllShiftsWithPositions())
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to retrieve schedules", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).getAllShiftsWithPositions();
    }

    @Test
    void getAllSchedules_ServiceReturnsNull_HandlesGracefully() {
        // Arrange
        when(scheduledShiftService.getAllShiftsWithPositions()).thenReturn(null);

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Schedules retrieved successfully", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    // ========== GET SCHEDULES BY EMPLOYEE TESTS ==========

    @Test
    void getSchedulesByEmployee_Success() {
        // Arrange
        List<EmployeeShiftDTO> shifts = Arrays.asList(testShiftDTO);
        when(scheduledShiftService.getShiftsByEmployeeId(1L)).thenReturn(shifts);

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Employee schedules retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(1L, response.getBody().getData().get(0).getEmployeeId());
        assertEquals("John Doe", response.getBody().getData().get(0).getFullName());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(1L);
    }

    @Test
    void getSchedulesByEmployee_EmptyList() {
        // Arrange
        when(scheduledShiftService.getShiftsByEmployeeId(1L)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Employee schedules retrieved successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData().isEmpty());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(1L);
    }

    @Test
    void getSchedulesByEmployee_NullEmployeeId_ReturnsBadRequest() {
        // Arrange
        when(scheduledShiftService.getShiftsByEmployeeId(null))
                .thenThrow(new IllegalArgumentException("Employee ID cannot be null"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee ID cannot be null", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(null);
    }

    @Test
    void getSchedulesByEmployee_EmployeeNotFound_ReturnsBadRequest() {
        // Arrange
        when(scheduledShiftService.getShiftsByEmployeeId(999L))
                .thenThrow(new IllegalArgumentException("Employee not found with ID: 999"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(999L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(999L);
    }

    @Test
    void getSchedulesByEmployee_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        when(scheduledShiftService.getShiftsByEmployeeId(1L))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to retrieve employee schedules", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(1L);
    }

    // ========== EXCEPTION MAPPING TESTS ==========

    @Test
    void getSchedulesByEmployee_WhenServiceThrowsDifferentExceptions_MapsToCorrectHttpStatus() {
        // Test IllegalArgumentException -> BAD_REQUEST
        when(scheduledShiftService.getShiftsByEmployeeId(1L))
                .thenThrow(new IllegalArgumentException("Invalid ID"));
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response1 =
                scheduledShiftController.getSchedulesByEmployee(1L);
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        assertEquals("Invalid ID", response1.getBody().getMessage());
        assertNull(response1.getBody().getData());
        
        // Test RuntimeException -> INTERNAL_SERVER_ERROR
        when(scheduledShiftService.getShiftsByEmployeeId(2L))
                .thenThrow(new RuntimeException("Database error"));
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response2 =
                scheduledShiftController.getSchedulesByEmployee(2L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response2.getStatusCode());
        assertEquals("Failed to retrieve employee schedules", response2.getBody().getMessage());
        assertNull(response2.getBody().getData());
        
        // Test NullPointerException -> INTERNAL_SERVER_ERROR
        when(scheduledShiftService.getShiftsByEmployeeId(3L))
                .thenThrow(new NullPointerException("Null reference"));
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response3 =
                scheduledShiftController.getSchedulesByEmployee(3L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response3.getStatusCode());
        assertEquals("Failed to retrieve employee schedules", response3.getBody().getMessage());
        assertNull(response3.getBody().getData());
    }

    // ========== API RESPONSE STRUCTURE TESTS ==========

    @Test
    void getAllSchedules_VerifyApiResponseStructure() {
        // Arrange
        when(scheduledShiftService.getAllShiftsWithPositions())
                .thenReturn(Collections.singletonList(testShiftDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        ApiResponse<List<EmployeeShiftDTO>> apiResponse = response.getBody();
        
        assertAll("Verify API response structure",
            () -> assertNotNull(apiResponse),
            () -> assertTrue(apiResponse.isSuccess()),
            () -> assertNotNull(apiResponse.getData()),
            () -> assertEquals(1, apiResponse.getData().size()),
            () -> assertEquals("Schedules retrieved successfully", apiResponse.getMessage())
        );
    }

    @Test
    void getSchedulesByEmployee_ErrorResponse_IncludesErrorMessage() {
        // Arrange
        String errorMessage = "Employee not found with ID: 999";
        when(scheduledShiftService.getShiftsByEmployeeId(999L))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(999L);

        // Assert
        ApiResponse<List<EmployeeShiftDTO>> apiResponse = response.getBody();
        
        assertAll("Verify error response structure",
            () -> assertNotNull(apiResponse),
            () -> assertFalse(apiResponse.isSuccess()),
            () -> assertNull(apiResponse.getData()),
            () -> assertEquals(errorMessage, apiResponse.getMessage())
        );
    }

    // ========== DTO FIELD INTEGRITY TESTS ==========

    @Test
    void getAllSchedules_VerifyAllFieldsArePopulated() {
        // Arrange
        when(scheduledShiftService.getAllShiftsWithPositions())
                .thenReturn(Collections.singletonList(testShiftDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        EmployeeShiftDTO dto = response.getBody().getData().get(0);
        
        assertAll("Verify all DTO fields are correctly populated",
            () -> assertEquals(1L, dto.getEmployeeId()),
            () -> assertEquals("John Doe", dto.getFullName()),
            () -> assertEquals("Cashier", dto.getPositionName()),
            () -> assertEquals(new BigDecimal("18.50"), dto.getHourlyWage()),
            () -> assertEquals(1L, dto.getShiftId()),
            () -> assertEquals(LocalDate.of(2025, 6, 2), dto.getDate()),
            () -> assertEquals(DayOfWeek.MONDAY, dto.getDayOfWeek()),
            () -> assertEquals(LocalTime.of(9, 0), dto.getStartTime()),
            () -> assertEquals(LocalTime.of(17, 0), dto.getEndTime())
        );
    }

    @Test
    void getAllSchedules_UnassignedPosition_ReturnsCorrectly() {
        // Arrange — employee with no position (service defaults to "Unassigned" / ZERO wage)
        EmployeeShiftDTO unassignedDTO = new EmployeeShiftDTO(
                3L,
                "Bob Unassigned",
                "Unassigned",
                BigDecimal.ZERO,
                3L,
                LocalDate.of(2025, 6, 4),
                DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );
        when(scheduledShiftService.getAllShiftsWithPositions())
                .thenReturn(Collections.singletonList(unassignedDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getAllSchedules();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Unassigned", response.getBody().getData().get(0).getPositionName());
        assertEquals(BigDecimal.ZERO, response.getBody().getData().get(0).getHourlyWage());
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    void getAllSchedules_ShouldCompleteWithinReasonableTime() {
        // Arrange
        List<EmployeeShiftDTO> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(new EmployeeShiftDTO(
                (long) i, 
                "Employee " + i, 
                "Position", 
                BigDecimal.TEN,
                (long) i, 
                LocalDate.now(), 
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0), 
                LocalTime.of(17, 0)
            ));
        }
        when(scheduledShiftService.getAllShiftsWithPositions()).thenReturn(largeList);

        // Act & Assert
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                    scheduledShiftController.getAllSchedules();
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1000, response.getBody().getData().size());
        });
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void getSchedulesByEmployee_NegativeEmployeeId_ReturnsBadRequest() {
        // Arrange
        when(scheduledShiftService.getShiftsByEmployeeId(-1L))
                .thenThrow(new IllegalArgumentException("Employee ID cannot be negative"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(-1L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee ID cannot be negative", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(-1L);
    }

    @Test
    void getSchedulesByEmployee_ZeroEmployeeId_ReturnsSuccess() {
        // Arrange - Some systems might use 0 as valid ID
        List<EmployeeShiftDTO> shifts = Arrays.asList(testShiftDTO);
        when(scheduledShiftService.getShiftsByEmployeeId(0L)).thenReturn(shifts);

        // Act
        ResponseEntity<ApiResponse<List<EmployeeShiftDTO>>> response =
                scheduledShiftController.getSchedulesByEmployee(0L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Employee schedules retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        verify(scheduledShiftService, times(1)).getShiftsByEmployeeId(0L);
    }
}