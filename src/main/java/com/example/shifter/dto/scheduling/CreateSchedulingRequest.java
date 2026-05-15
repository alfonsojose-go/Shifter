package com.example.shifter.dto.scheduling;

import java.util.List;

import com.example.shifter.model.scheduling.Scheduling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class CreateSchedulingRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @Valid
    @NotNull(message = "Scheduling list is required")
    private List<SchedulingSlot> scheduling;


    //getters and setters
    public Long getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
    public List<SchedulingSlot> getSchedules() {
        return scheduling;
    }
    public void setSchedules(List<SchedulingSlot> scheduling) {
        this.scheduling = scheduling;
    }

    @Data
    @NoArgsConstructor
    @Getter
    @Setter
    public static class SchedulingSlot {

        @NotNull(message = "Day of week is required")
        private Scheduling.DayOfWeek dayOfWeek;

        @NotBlank(message = "Start time is required")
        private String startTime;

        @NotBlank(message = "End time is required")
        private String endTime;


        public SchedulingSlot(Scheduling.DayOfWeek dayOfWeek, String startTime, String endTime) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
        }



    }
}
