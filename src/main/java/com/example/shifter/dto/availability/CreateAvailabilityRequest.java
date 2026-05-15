package com.example.shifter.dto.availability;

import java.util.List;

import com.example.shifter.model.availability.Availability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAvailabilityRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @Valid
    @NotNull(message = "Availabilities list is required")
    private List<AvailabilitySlot> availabilities;


    //getters and setters
    public Long getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
    public List<AvailabilitySlot> getAvailabilities() {
        return availabilities;
    }
    public void setAvailabilities(List<AvailabilitySlot> availabilities) {
        this.availabilities = availabilities;
    }

    @Data
    public static class AvailabilitySlot {

        @NotNull(message = "Day of week is required")
        private Availability.DayOfWeek dayOfWeek;

        @NotBlank(message = "Start time is required")
        private String startTime;

        @NotBlank(message = "End time is required")
        private String endTime;


        //Constructor
        public AvailabilitySlot() {
        }

        public AvailabilitySlot(Availability.DayOfWeek dayOfWeek, String startTime, String endTime) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
        }




        //getters and setters
        public Availability.DayOfWeek getDayOfWeek() {
            return dayOfWeek;       
        }
        public void setDayOfWeek(Availability.DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }
        public String getStartTime() {
            return startTime;
        }
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
        public String getEndTime() {
            return endTime;
        } 
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

    }
}
