package com.example.shifter.service.scheduling;

import java.util.List;

import com.example.shifter.dto.scheduling.CreateSchedulingRequest.SchedulingSlot;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;

public interface SchedulingService {

    // Create Schedule by batch or single request
    void createSchedule(Long employeeId, List<SchedulingSlot> slots);


    // Delete schedule by ID
    void deleteScheduleById(Long schedulingId);

    //Delete all schedule of an employee
    void deleteAllScheduleOfEmployee(Long employeeId);

    //Update schedule by Id
    void updateScheduleById(Long schedulingId, 
                                Scheduling.DayOfWeek dayOfWeek, 
                                String startTime, 
                                String endTime);

    
    // Get schedule by ID
    Scheduling getScheduleById(Long schedulingId);
    
    // Get all schedules for an employee
    List<Scheduling> getSchedulesByEmployee(Long employeeId);
    
    // Get schedules for an employee on specific day
    List<Scheduling> getSchedulesByEmployeeAndDay(Long employeeId, 
                                                        Scheduling.DayOfWeek dayOfWeek);
    
    // Get all schedules (for managers)
    List<Scheduling> getAllSchedules();
    
    // Check if employee has any schedule
    boolean hasSchedule(Long employeeId);
    
    // Get available employees by day
    List<User> getAvailableEmployeesByDay(Scheduling.DayOfWeek dayOfWeek);
    
    // Convert time string to LocalTime
    java.time.LocalTime parseTimeString(String timeString);
    
    // Validate time range
    void validateTimeRange(String startTime, String endTime);

}
