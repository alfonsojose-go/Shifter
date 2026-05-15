package com.example.shifter.dto.scheduling;

import com.example.shifter.model.scheduling.Scheduling;

import lombok.Data;

@Data
public class SchedulingResponse {
    private Long schedulingId;
        private Long employeeId;
        private String employeeName;
        private String employeeUsername;
        private Scheduling.DayOfWeek dayOfWeek;
        private String startTime;
        private String endTime;
        private String createdAt;
        private String updatedAt;
        
        public static SchedulingResponse fromScheduling(Scheduling scheduling) {
            SchedulingResponse response = new SchedulingResponse();
            response.setSchedulingId(scheduling.getSchedulingId());
            
            if (scheduling.getEmployee() != null) {
                response.setEmployeeId(scheduling.getEmployee().getId());
                response.setEmployeeName(scheduling.getEmployee().getFullName());
                response.setEmployeeUsername(scheduling.getEmployee().getUsername());
            }
            
            response.setDayOfWeek(scheduling.getDayOfWeek());
            response.setStartTime(scheduling.getStartTime().toString());
            response.setEndTime(scheduling.getEndTime().toString());
            
            if (scheduling.getCreatedAt() != null) {
                response.setCreatedAt(scheduling.getCreatedAt().toString());
            }
            
            if (scheduling.getUpdatedAt() != null) {
                response.setUpdatedAt(scheduling.getUpdatedAt().toString());
            }
            
            return response;
        }
}
