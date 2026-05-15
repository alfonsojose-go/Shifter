package com.example.shifter.dto.availability;

import com.example.shifter.model.availability.Availability;

import lombok.Data;

@Data
public class AvailabilityResponse {
    private Long availabilityId;
        private Long employeeId;
        private String employeeName;
        private String employeeUsername;
        private Availability.DayOfWeek dayOfWeek;
        private String startTime;
        private String endTime;
        private String createdAt;
        private String updatedAt;
        
        public static AvailabilityResponse fromAvailability(Availability availability) {
            AvailabilityResponse response = new AvailabilityResponse();
            response.setAvailabilityId(availability.getAvailabilityId());
            
            if (availability.getEmployee() != null) {
                response.setEmployeeId(availability.getEmployee().getId());
                response.setEmployeeName(availability.getEmployee().getFullName());
                response.setEmployeeUsername(availability.getEmployee().getUsername());
            }
            
            response.setDayOfWeek(availability.getDayOfWeek());
            response.setStartTime(availability.getStartTime().toString());
            response.setEndTime(availability.getEndTime().toString());
            
            if (availability.getCreatedAt() != null) {
                response.setCreatedAt(availability.getCreatedAt().toString());
            }
            
            if (availability.getUpdatedAt() != null) {
                response.setUpdatedAt(availability.getUpdatedAt().toString());
            }
            
            return response;
        }
}
