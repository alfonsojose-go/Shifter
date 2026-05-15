package com.example.shifter.controller.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;
import com.example.shifter.dto.availability.ApiResponse;
import com.example.shifter.model.scheduling.Scheduling.DayOfWeek;
import com.example.shifter.service.position_scheduling.PositionSchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionSchedulingControllerTest {

    @Mock
    private PositionSchedulingService positionSchedulingService;

    @InjectMocks
    private PositionSchedulingController positionSchedulingController;

    private EmployeeScheduleDTO testScheduleDTO;
    private EmployeeScheduleDTO testScheduleDTO2;

    @BeforeEach
    void setUp() {
        testScheduleDTO = new EmployeeScheduleDTO(
                1L,
                "John Doe",
                "Cashier",
                new BigDecimal("18.50"),
                1L,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        testScheduleDTO2 = new EmployeeScheduleDTO(
                2L,
                "Jane Smith",
                "Barista",
                new BigDecimal("17.00"),
                2L,
                DayOfWeek.TUESDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0)
        );
    }

    // ========== GET ALL SCHEDULES TESTS ==========
    // Note: Controller method is called getAllSchedules(), not getAllSchedulesWithPositions()

    @Test
    void getAllSchedules_Success() {
            // Arrange
            List<EmployeeScheduleDTO> schedules = Arrays.asList(testScheduleDTO, testScheduleDTO2);
            when(positionSchedulingService.getAllSchedulesWithPositions()).thenReturn(schedules);

            // Act
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response = positionSchedulingController
                            .getAllSchedules();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(2, response.getBody().getData().size());
            assertEquals("John Doe", response.getBody().getData().get(0).getFullName());
            assertEquals("Cashier", response.getBody().getData().get(0).getPositionName());
            assertEquals(DayOfWeek.MONDAY, response.getBody().getData().get(0).getDayOfWeek());
            assertEquals("Schedules retrieved successfully", response.getBody().getMessage()); // FIXED: not null
            verify(positionSchedulingService, times(1)).getAllSchedulesWithPositions();
    }


    @Test
    void getAllSchedules_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        when(positionSchedulingService.getAllSchedulesWithPositions())
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to retrieve schedules", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(positionSchedulingService, times(1)).getAllSchedulesWithPositions();
    }

    @Test
    void getAllSchedules_ServiceReturnsNull_HandlesGracefully() {
            // Arrange
            when(positionSchedulingService.getAllSchedulesWithPositions()).thenReturn(null);

            // Act
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response = positionSchedulingController
                            .getAllSchedules();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertNull(response.getBody().getData()); // Data is null
            assertEquals("Schedules retrieved successfully", response.getBody().getMessage()); // Message is NOT null
            verify(positionSchedulingService, times(1)).getAllSchedulesWithPositions();
    }

    // ========== GET SCHEDULES BY EMPLOYEE TESTS ==========
    // Note: This method name is correct - getSchedulesByEmployee()

    @Test
    void getSchedulesByEmployee_Success() {
            // Arrange
            List<EmployeeScheduleDTO> schedules = Arrays.asList(testScheduleDTO);
            when(positionSchedulingService.getSchedulesByEmployeeId(1L)).thenReturn(schedules);

            // Act
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response = positionSchedulingController
                            .getSchedulesByEmployee(1L);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().isSuccess());
            assertEquals(1, response.getBody().getData().size());
            assertEquals(1L, response.getBody().getData().get(0).getEmployeeId());
            assertEquals("John Doe", response.getBody().getData().get(0).getFullName());
            assertEquals(DayOfWeek.MONDAY, response.getBody().getData().get(0).getDayOfWeek());
            assertEquals("Employee schedules retrieved successfully", response.getBody().getMessage()); // FIXED: not
                                                                                                        // null
            verify(positionSchedulingService, times(1)).getSchedulesByEmployeeId(1L);
    }

    
    @Test
    void getAllSchedules_EmptyList() {
            // Arrange
            when(positionSchedulingService.getAllSchedulesWithPositions())
                            .thenReturn(Collections.emptyList());

            // Act
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response = positionSchedulingController
                            .getAllSchedules();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertTrue(response.getBody().getData().isEmpty());
            assertEquals("Schedules retrieved successfully", response.getBody().getMessage()); // FIXED
            verify(positionSchedulingService, times(1)).getAllSchedulesWithPositions();
    }

    @Test
    void getSchedulesByEmployee_NullEmployeeId_ReturnsBadRequest() {
        // Arrange
        when(positionSchedulingService.getSchedulesByEmployeeId(null))
                .thenThrow(new IllegalArgumentException("Employee ID cannot be null"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getSchedulesByEmployee(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee ID cannot be null", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(positionSchedulingService, times(1)).getSchedulesByEmployeeId(null);
    }

    @Test
    void getSchedulesByEmployee_NegativeEmployeeId_ReturnsBadRequest() {
        // Arrange
        when(positionSchedulingService.getSchedulesByEmployeeId(-1L))
                .thenThrow(new IllegalArgumentException("Employee ID cannot be negative"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getSchedulesByEmployee(-1L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee ID cannot be negative", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(positionSchedulingService, times(1)).getSchedulesByEmployeeId(-1L);
    }

    @Test
    void getSchedulesByEmployee_EmployeeNotFound_ReturnsBadRequest() {
        // Arrange
        when(positionSchedulingService.getSchedulesByEmployeeId(999L))
                .thenThrow(new IllegalArgumentException("Employee not found with ID: 999"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getSchedulesByEmployee(999L);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(positionSchedulingService, times(1)).getSchedulesByEmployeeId(999L);
    }

    @Test
    void getSchedulesByEmployee_UnexpectedException_ReturnsInternalServerError() {
        // Arrange
        when(positionSchedulingService.getSchedulesByEmployeeId(1L))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getSchedulesByEmployee(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to retrieve employee schedules", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(positionSchedulingService, times(1)).getSchedulesByEmployeeId(1L);
    }

    // ========== EXCEPTION MAPPING TESTS ==========

    @Test
    void getSchedulesByEmployee_WhenServiceThrowsDifferentExceptions_MapsToCorrectHttpStatus() {
        // Test IllegalArgumentException -> BAD_REQUEST
        when(positionSchedulingService.getSchedulesByEmployeeId(1L))
                .thenThrow(new IllegalArgumentException("Invalid ID"));
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response1 =
                positionSchedulingController.getSchedulesByEmployee(1L);
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        assertEquals("Invalid ID", response1.getBody().getMessage());
        assertNull(response1.getBody().getData());
        
        // Test RuntimeException -> INTERNAL_SERVER_ERROR
        when(positionSchedulingService.getSchedulesByEmployeeId(2L))
                .thenThrow(new RuntimeException("Database error"));
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response2 =
                positionSchedulingController.getSchedulesByEmployee(2L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response2.getStatusCode());
        assertEquals("Failed to retrieve employee schedules", response2.getBody().getMessage());
        assertNull(response2.getBody().getData());
        
        // Test NullPointerException -> INTERNAL_SERVER_ERROR
        when(positionSchedulingService.getSchedulesByEmployeeId(3L))
                .thenThrow(new NullPointerException("Null reference"));
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response3 =
                positionSchedulingController.getSchedulesByEmployee(3L);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response3.getStatusCode());
        assertEquals("Failed to retrieve employee schedules", response3.getBody().getMessage());
        assertNull(response3.getBody().getData());
    }

    // ========== API RESPONSE STRUCTURE TESTS ==========

    @Test
    void getAllSchedules_VerifyApiResponseStructure() {
            // Arrange
            when(positionSchedulingService.getAllSchedulesWithPositions())
                            .thenReturn(Collections.singletonList(testScheduleDTO));

            // Act
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response = positionSchedulingController
                            .getAllSchedules();

            // Assert
            ApiResponse<List<EmployeeScheduleDTO>> apiResponse = response.getBody();

            assertAll("Verify API response structure",
                            () -> assertNotNull(apiResponse),
                            () -> assertTrue(apiResponse.isSuccess()),
                            () -> assertNotNull(apiResponse.getData()),
                            () -> assertEquals(1, apiResponse.getData().size()),
                            () -> assertEquals("Schedules retrieved successfully", apiResponse.getMessage()) // FIXED:
                                                                                                             // not null
            );
    }

    @Test
    void getSchedulesByEmployee_ErrorResponse_IncludesErrorMessage() {
        // Arrange
        String errorMessage = "Employee not found with ID: 999";
        when(positionSchedulingService.getSchedulesByEmployeeId(999L))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getSchedulesByEmployee(999L);

        // Assert
        ApiResponse<List<EmployeeScheduleDTO>> apiResponse = response.getBody();
        
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
        when(positionSchedulingService.getAllSchedulesWithPositions())
                .thenReturn(Collections.singletonList(testScheduleDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        EmployeeScheduleDTO dto = response.getBody().getData().get(0);
        
        assertAll("Verify all DTO fields are correctly populated",
            () -> assertEquals(1L, dto.getEmployeeId()),
            () -> assertEquals("John Doe", dto.getFullName()),
            () -> assertEquals("Cashier", dto.getPositionName()),
            () -> assertEquals(new BigDecimal("18.50"), dto.getHourlyWage()),
            () -> assertEquals(1L, dto.getScheduleId()),
            () -> assertEquals(DayOfWeek.MONDAY, dto.getDayOfWeek()),
            () -> assertEquals(LocalTime.of(9, 0), dto.getStartTime()),
            () -> assertEquals(LocalTime.of(17, 0), dto.getEndTime())
        );
    }

    @Test
    void getAllSchedules_UnassignedPosition_ReturnsCorrectly() {
        // Arrange — employee with no position (service defaults to "Unassigned" / ZERO wage)
        EmployeeScheduleDTO unassignedDTO = new EmployeeScheduleDTO(
                3L,
                "Bob Unassigned",
                "Unassigned",
                BigDecimal.ZERO,
                3L,
                DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );
        when(positionSchedulingService.getAllSchedulesWithPositions())
                .thenReturn(Collections.singletonList(unassignedDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Unassigned", response.getBody().getData().get(0).getPositionName());
        assertEquals(BigDecimal.ZERO, response.getBody().getData().get(0).getHourlyWage());
    }

    @Test
    void getAllSchedules_HourlyWageIsCorrect() {
        // Arrange
        when(positionSchedulingService.getAllSchedulesWithPositions())
                .thenReturn(Collections.singletonList(testScheduleDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        assertEquals(new BigDecimal("18.50"),
                response.getBody().getData().get(0).getHourlyWage());
    }

    @Test
    void getAllSchedules_MultipleEmployees_AllDaysRepresented() {
        // Arrange
        List<EmployeeScheduleDTO> schedules = Arrays.asList(testScheduleDTO, testScheduleDTO2);
        when(positionSchedulingService.getAllSchedulesWithPositions()).thenReturn(schedules);

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        List<EmployeeScheduleDTO> data = response.getBody().getData();
        assertEquals(DayOfWeek.MONDAY, data.get(0).getDayOfWeek());
        assertEquals(DayOfWeek.TUESDAY, data.get(1).getDayOfWeek());
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    void getAllSchedules_ShouldCompleteWithinReasonableTime() {
        // Arrange
        List<EmployeeScheduleDTO> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(new EmployeeScheduleDTO(
                (long) i,
                "Employee " + i,
                "Position",
                BigDecimal.TEN,
                (long) i,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
            ));
        }
        when(positionSchedulingService.getAllSchedulesWithPositions()).thenReturn(largeList);

        // Act & Assert
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                    positionSchedulingController.getAllSchedules();  // FIXED: method name
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1000, response.getBody().getData().size());
        });
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void getSchedulesByEmployee_ZeroEmployeeId_ReturnsSuccess() {
            // Arrange - Some systems might use 0 as valid ID
            List<EmployeeScheduleDTO> schedules = Arrays.asList(testScheduleDTO);
            when(positionSchedulingService.getSchedulesByEmployeeId(0L)).thenReturn(schedules);

            // Act
            ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response = positionSchedulingController
                            .getSchedulesByEmployee(0L);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isSuccess());
            assertEquals(1, response.getBody().getData().size());
            assertEquals("Employee schedules retrieved successfully", response.getBody().getMessage()); // FIXED: not
                                                                                                        // null
            verify(positionSchedulingService, times(1)).getSchedulesByEmployeeId(0L);
    }

    @Test
    void getAllSchedules_ExtremelyLargeWageValue_HandlesCorrectly() {
        // Arrange
        BigDecimal hugeWage = new BigDecimal("999999.99");
        EmployeeScheduleDTO highWageDTO = new EmployeeScheduleDTO(
                4L,
                "Rich Employee",
                "Manager",
                hugeWage,
                4L,
                DayOfWeek.FRIDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );
        when(positionSchedulingService.getAllSchedulesWithPositions())
                .thenReturn(Collections.singletonList(highWageDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(hugeWage, response.getBody().getData().get(0).getHourlyWage());
    }

    @Test
    void getAllSchedules_MidnightShiftTimes_HandlesCorrectly() {
        // Arrange
        EmployeeScheduleDTO midnightShiftDTO = new EmployeeScheduleDTO(
                5L,
                "Night Worker",
                "Security",
                new BigDecimal("20.00"),
                5L,
                DayOfWeek.SATURDAY,
                LocalTime.of(0, 0),
                LocalTime.of(8, 0)
        );
        when(positionSchedulingService.getAllSchedulesWithPositions())
                .thenReturn(Collections.singletonList(midnightShiftDTO));

        // Act
        ResponseEntity<ApiResponse<List<EmployeeScheduleDTO>>> response =
                positionSchedulingController.getAllSchedules();  // FIXED: method name

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(LocalTime.of(0, 0), response.getBody().getData().get(0).getStartTime());
        assertEquals(LocalTime.of(8, 0), response.getBody().getData().get(0).getEndTime());
    }
}