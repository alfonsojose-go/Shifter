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

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

        @Mock
        private AvailabilityService availabilityService;

        @InjectMocks
        private AvailabilityController availabilityController;

        private User testUser;
        private Availability testAvailability;
        private AvailabilityResponse testAvailabilityResponse;
        private EmployeeResponse testEmployeeResponse;


        @BeforeEach
        void setUp() {
                testUser = new User();
                testUser.setId(1L);
                testUser.setFullName("John Doe");
                testUser.setUsername("johndoe");
                testUser.setEmail("john@example.com");

                testAvailability = new Availability();
                testAvailability.setAvailabilityId(1L);
                testAvailability.setEmployee(testUser);
                testAvailability.setDayOfWeek(Availability.DayOfWeek.MONDAY);
                testAvailability.setStartTime(LocalTime.of(9, 0));
                testAvailability.setEndTime(LocalTime.of(17, 0));

                testAvailabilityResponse = new AvailabilityResponse();
                testAvailabilityResponse.setAvailabilityId(1L);
                testAvailabilityResponse.setEmployeeId(1L);
                testAvailabilityResponse.setEmployeeName("John Doe");
                testAvailabilityResponse.setDayOfWeek(Availability.DayOfWeek.MONDAY);
                testAvailabilityResponse.setStartTime("09:00 AM");
                testAvailabilityResponse.setEndTime("05:00 PM");

                testEmployeeResponse = new EmployeeResponse();
                testEmployeeResponse.setId(1L);
                testEmployeeResponse.setFullName("John Doe");
                testEmployeeResponse.setUsername("johndoe");
                testEmployeeResponse.setEmail("john@example.com");
        }

        // ========== CREATE AVAILABILITY TESTS ==========
        @Test
        void createAvailability_Success() {
                // Arrange
                CreateAvailabilityRequest.AvailabilitySlot requestSlot = new CreateAvailabilityRequest.AvailabilitySlot();
                requestSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
                requestSlot.setStartTime("09:00");
                requestSlot.setEndTime("17:00");

                CreateAvailabilityRequest request = new CreateAvailabilityRequest();
                request.setEmployeeId(1L);
                request.setAvailabilities(Collections.singletonList(requestSlot));

                doNothing().when(availabilityService).createAvailability(eq(1L), anyList());

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

                // Assert
                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().isSuccess());
                assertTrue(response.getBody().getMessage().contains("Successfully created"));
                verify(availabilityService, times(1)).createAvailability(eq(1L), anyList());
        }

        @Test
        void createAvailability_WithEmptySlots_ClearsAvailabilities() {
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
                verify(availabilityService, times(1)).createAvailability(eq(1L), eq(Collections.emptyList()));
        }

        @Test
        void createAvailability_InvalidInput_ReturnsBadRequest() {
                // Arrange
                CreateAvailabilityRequest.AvailabilitySlot requestSlot = new CreateAvailabilityRequest.AvailabilitySlot();
                requestSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
                requestSlot.setStartTime("invalid-time");
                requestSlot.setEndTime("17:00");

                CreateAvailabilityRequest request = new CreateAvailabilityRequest();
                request.setEmployeeId(1L);
                request.setAvailabilities(Collections.singletonList(requestSlot));

                doThrow(new IllegalArgumentException("Invalid time format"))
                                .when(availabilityService).createAvailability(eq(1L), anyList());

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertNotNull(response.getBody());
                assertFalse(response.getBody().isSuccess());
                assertEquals("Invalid time format", response.getBody().getMessage());
        }

        @Test
        void createAvailability_OverlapError_ReturnsBadRequest() {
                // Arrange
                CreateAvailabilityRequest.AvailabilitySlot requestSlot = new CreateAvailabilityRequest.AvailabilitySlot();
                requestSlot.setDayOfWeek(Availability.DayOfWeek.MONDAY);
                requestSlot.setStartTime("09:00");
                requestSlot.setEndTime("17:00");

                CreateAvailabilityRequest request = new CreateAvailabilityRequest();
                request.setEmployeeId(1L);
                request.setAvailabilities(Collections.singletonList(requestSlot));

                doThrow(new IllegalArgumentException("Overlaps with existing MONDAY slot: 09:00 AM - 05:00 PM"))
                                .when(availabilityService).createAvailability(eq(1L), anyList());

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertTrue(response.getBody().getMessage().contains("Overlaps"));
        }

        // ========== GET ALL AVAILABILITIES TESTS ==========
        @Test
        void getAllAvailabilities_Success() {
                // Arrange
                List<Availability> availabilities = Arrays.asList(testAvailability);
                when(availabilityService.getAllAvailabilities()).thenReturn(availabilities);

                // Act
                ResponseEntity<ApiResponse<List<AvailabilityResponse>>> response = availabilityController
                                .getAllAvailabilities();

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Availabilities retrieved successfully", response.getBody().getMessage());
                assertNotNull(response.getBody().getData());
                assertEquals(1, response.getBody().getData().size());
                verify(availabilityService, times(1)).getAllAvailabilities();
        }

        @Test
        void getAllAvailabilities_EmptyList() {
                // Arrange
                when(availabilityService.getAllAvailabilities()).thenReturn(Collections.emptyList());

                // Act
                ResponseEntity<ApiResponse<List<AvailabilityResponse>>> response = availabilityController
                                .getAllAvailabilities();

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertTrue(response.getBody().getData().isEmpty());
        }

        // ========== GET AVAILABILITY BY ID TESTS ==========
        @Test
        void getAvailabilityById_Success() {
                // Arrange
                when(availabilityService.getAvailabilityById(1L)).thenReturn(testAvailability);

                // Act
                ResponseEntity<ApiResponse<AvailabilityResponse>> response = availabilityController
                                .getAvailabilityById(1L);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Availability retrieved successfully", response.getBody().getMessage());
                assertNotNull(response.getBody().getData());
                verify(availabilityService, times(1)).getAvailabilityById(1L);
        }

        @Test
        void getAvailabilityById_NotFound() {
                // Arrange
                when(availabilityService.getAvailabilityById(999L))
                                .thenThrow(new IllegalArgumentException("Availability not found with ID: 999"));

                // Act
                ResponseEntity<ApiResponse<AvailabilityResponse>> response = availabilityController
                                .getAvailabilityById(999L);

                // Assert
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertEquals("Availability not found with ID: 999", response.getBody().getMessage());
                assertNull(response.getBody().getData());
        }

        // ========== GET AVAILABILITIES BY EMPLOYEE TESTS ==========
        @Test
        void getAvailabilitiesByEmployee_Success() {
                // Arrange
                List<Availability> availabilities = Arrays.asList(testAvailability);
                when(availabilityService.getAvailabilitiesByEmployee(1L)).thenReturn(availabilities);

                // Act
                ResponseEntity<ApiResponse<List<AvailabilityResponse>>> response = availabilityController
                                .getAvailabilitiesByEmployee(1L);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Employee availabilities retrieved successfully", response.getBody().getMessage());
                assertEquals(1, response.getBody().getData().size());
                verify(availabilityService, times(1)).getAvailabilitiesByEmployee(1L);
        }

        @Test
        void getAvailabilitiesByEmployee_EmployeeNotFound() {
                // Arrange
                when(availabilityService.getAvailabilitiesByEmployee(999L))
                                .thenThrow(new IllegalArgumentException("Employee not found with ID: 999"));

                // Act
                ResponseEntity<ApiResponse<List<AvailabilityResponse>>> response = availabilityController
                                .getAvailabilitiesByEmployee(999L);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
        }

        // ========== GET AVAILABILITIES BY EMPLOYEE AND DAY TESTS ==========
        @Test
        void getAvailabilitiesByEmployeeAndDay_Success() {
                // Arrange
                List<Availability> availabilities = Arrays.asList(testAvailability);
                when(availabilityService.getAvailabilitiesByEmployeeAndDay(1L, Availability.DayOfWeek.MONDAY))
                                .thenReturn(availabilities);

                // Act
                ResponseEntity<ApiResponse<List<AvailabilityResponse>>> response = availabilityController
                                .getAvailabilitiesByEmployeeAndDay(1L, Availability.DayOfWeek.MONDAY);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Employee day availabilities retrieved successfully", response.getBody().getMessage());
                assertEquals(1, response.getBody().getData().size());
                verify(availabilityService, times(1)).getAvailabilitiesByEmployeeAndDay(1L,
                                Availability.DayOfWeek.MONDAY);
        }

        // ========== UPDATE AVAILABILITY TESTS ==========
        @Test
        void updateAvailability_Success() {
                // Arrange
                UpdateAvailabilityRequest request = new UpdateAvailabilityRequest();
                request.setDayOfWeek(Availability.DayOfWeek.TUESDAY);
                request.setStartTime("10:00");
                request.setEndTime("18:00");

                doNothing().when(availabilityService).updateAvailabilityById(
                                eq(1L), eq(Availability.DayOfWeek.TUESDAY), eq("10:00"), eq("18:00"));

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.updateAvailability(1L, request);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Availability updated successfully", response.getBody().getMessage());
                verify(availabilityService, times(1)).updateAvailabilityById(1L,
                                Availability.DayOfWeek.TUESDAY, "10:00", "18:00");
        }

        @Test
        void updateAvailability_OverlapError() {
                // Arrange
                UpdateAvailabilityRequest request = new UpdateAvailabilityRequest();
                request.setDayOfWeek(Availability.DayOfWeek.MONDAY);
                request.setStartTime("09:00");
                request.setEndTime("17:00");

                doThrow(new IllegalArgumentException("Overlaps with existing MONDAY slot: 09:00 AM - 05:00 PM"))
                                .when(availabilityService)
                                .updateAvailabilityById(eq(1L), any(), anyString(), anyString());

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.updateAvailability(1L, request);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertTrue(response.getBody().getMessage().contains("Overlaps"));
        }

        // ========== DELETE AVAILABILITY TESTS ==========
        @Test
        void deleteAvailability_Success() {
                // Arrange
                doNothing().when(availabilityService).deleteAvailabilityById(1L);

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.deleteAvailability(1L);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Availability deleted successfully", response.getBody().getMessage());
                verify(availabilityService, times(1)).deleteAvailabilityById(1L);
        }

        @Test
        void deleteAvailability_NotFound() {
                // Arrange
                doThrow(new IllegalArgumentException("Availability not found with ID: 999"))
                                .when(availabilityService).deleteAvailabilityById(999L);

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.deleteAvailability(999L);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertEquals("Availability not found with ID: 999", response.getBody().getMessage());
        }

        // ========== DELETE ALL AVAILABILITIES OF EMPLOYEE TESTS ==========
        @Test
        void deleteAllAvailabilitiesOfEmployee_Success() {
                // Arrange
                doNothing().when(availabilityService).deleteAllAvailabilitiesOfEmployee(1L);

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController
                                .deleteAllAvailabilitiesOfEmployee(1L);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("All availabilities for employee deleted successfully", response.getBody().getMessage());
                verify(availabilityService, times(1)).deleteAllAvailabilitiesOfEmployee(1L);
        }

        @Test
        void deleteAllAvailabilitiesOfEmployee_EmployeeNotFound() {
                // Arrange
                doThrow(new IllegalArgumentException("Employee not found with ID: 999"))
                                .when(availabilityService).deleteAllAvailabilitiesOfEmployee(999L);

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController
                                .deleteAllAvailabilitiesOfEmployee(999L);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertEquals("Employee not found with ID: 999", response.getBody().getMessage());
        }

        // ========== VALIDATE TIME RANGE TESTS ==========
        @Test
        void validateTimeRange_Valid() {
                // Arrange
                doNothing().when(availabilityService).validateTimeRange("09:00", "17:00");

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.validateTimeRange("09:00",
                                "17:00");

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Time range is valid", response.getBody().getMessage());
                verify(availabilityService, times(1)).validateTimeRange("09:00", "17:00");
        }

        @Test
        void validateTimeRange_Invalid() {
                // Arrange
                doThrow(new IllegalArgumentException("End time must be after start time"))
                                .when(availabilityService).validateTimeRange("17:00", "09:00");

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.validateTimeRange("17:00",
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
                when(availabilityService.getAvailableEmployeesByDay(Availability.DayOfWeek.MONDAY))
                                .thenReturn(employees);

                // Act
                ResponseEntity<ApiResponse<List<EmployeeResponse>>> response = availabilityController
                                .getAvailableEmployeesByDay(Availability.DayOfWeek.MONDAY);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Available employees retrieved successfully", response.getBody().getMessage());
                assertEquals(1, response.getBody().getData().size());
                assertEquals("John Doe", response.getBody().getData().get(0).getFullName());
                verify(availabilityService, times(1)).getAvailableEmployeesByDay(Availability.DayOfWeek.MONDAY);
        }

        @Test
        void getAvailableEmployeesByDay_EmptyList() {
                // Arrange
                when(availabilityService.getAvailableEmployeesByDay(Availability.DayOfWeek.SUNDAY))
                                .thenReturn(Collections.emptyList());

                // Act
                ResponseEntity<ApiResponse<List<EmployeeResponse>>> response = availabilityController
                                .getAvailableEmployeesByDay(Availability.DayOfWeek.SUNDAY);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertTrue(response.getBody().getData().isEmpty());
        }

        // ========== HAS AVAILABILITIES TESTS ==========
        @Test
        void hasAvailabilities_True() {
                // Arrange
                when(availabilityService.hasAvailabilities(1L)).thenReturn(true);

                // Act
                ResponseEntity<ApiResponse<Boolean>> response = availabilityController.hasAvailabilities(1L);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertEquals("Availability check completed successfully", response.getBody().getMessage());
                assertTrue(response.getBody().getData());
                verify(availabilityService, times(1)).hasAvailabilities(1L);
        }

        @Test
        void hasAvailabilities_False() {
                // Arrange
                when(availabilityService.hasAvailabilities(2L)).thenReturn(false);

                // Act
                ResponseEntity<ApiResponse<Boolean>> response = availabilityController.hasAvailabilities(2L);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().isSuccess());
                assertFalse(response.getBody().getData());
                verify(availabilityService, times(1)).hasAvailabilities(2L);
        }

        // ========== EXCEPTION HANDLING TESTS ==========
        @Test
        void createAvailability_UnexpectedException_ReturnsInternalServerError() {
                // Arrange
                CreateAvailabilityRequest request = new CreateAvailabilityRequest();
                request.setEmployeeId(1L);
                request.setAvailabilities(Collections.emptyList());

                doThrow(new RuntimeException("Database connection failed"))
                                .when(availabilityService).createAvailability(eq(1L), anyList());

                // Act
                ResponseEntity<ApiResponse<String>> response = availabilityController.createAvailability(request);

                // Assert
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        void getAllAvailabilities_UnexpectedException_ReturnsInternalServerError() {
                // Arrange
                when(availabilityService.getAllAvailabilities())
                                .thenThrow(new RuntimeException("Database error"));

                // Act
                ResponseEntity<ApiResponse<List<AvailabilityResponse>>> response = availabilityController
                                .getAllAvailabilities();

                // Assert
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertFalse(response.getBody().isSuccess());
                assertEquals("Failed to retrieve availabilities", response.getBody().getMessage());
        }
}