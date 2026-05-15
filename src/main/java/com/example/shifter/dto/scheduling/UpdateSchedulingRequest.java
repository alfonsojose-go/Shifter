package com.example.shifter.dto.scheduling;

import com.example.shifter.model.scheduling.Scheduling;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateSchedulingRequest {
    @NotNull(message = "Day of week is required")
    private Scheduling.DayOfWeek dayOfWeek;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "End time is required")
    private String endTime;
}
