package com.example.shifter.dto.scheduling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSchedulingRequest {
    
    @NotEmpty(message = "Employee schedules cannot be empty")
    private List<@Valid EmployeeScheduleRequest> employees;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeScheduleRequest {
        
        @NotNull(message = "Employee ID is required")
        private Long employeeId;
        
        private List<CreateSchedulingRequest.SchedulingSlot> schedules;
    }
}