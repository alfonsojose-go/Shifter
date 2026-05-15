package com.example.shifter.service.availability;

import com.example.shifter.dto.availability.CreateAvailabilityRequest.AvailabilitySlot;
import com.example.shifter.model.User;
import com.example.shifter.model.availability.Availability;

import java.util.List;


public interface AvailabilityService {

    // Create availability by batch or single request
    void createAvailability(Long employeeId, List<AvailabilitySlot> slots);


    // Delete availability by ID
    void deleteAvailabilityById(Long availabilityId);

    //Delete all availabilities of an employee
    void deleteAllAvailabilitiesOfEmployee(Long employeeId);

    //Update availability by Id
    void updateAvailabilityById(Long availabilityId, 
                                Availability.DayOfWeek dayOfWeek, 
                                String startTime, 
                                String endTime);

    
    // Get availability by ID
    Availability getAvailabilityById(Long availabilityId);
    
    // Get all availabilities for an employee
    List<Availability> getAvailabilitiesByEmployee(Long employeeId);
    
    // Get availabilities for an employee on specific day
    List<Availability> getAvailabilitiesByEmployeeAndDay(Long employeeId, 
                                                        Availability.DayOfWeek dayOfWeek);
    
    // Get all availabilities (for managers)
    List<Availability> getAllAvailabilities();
    
    // Check if employee has any availabilities
    boolean hasAvailabilities(Long employeeId);
    
    // Get available employees by day
    List<User> getAvailableEmployeesByDay(Availability.DayOfWeek dayOfWeek);
    
    // Convert time string to LocalTime
    java.time.LocalTime parseTimeString(String timeString);
    
    // Validate time range
    void validateTimeRange(String startTime, String endTime);


}