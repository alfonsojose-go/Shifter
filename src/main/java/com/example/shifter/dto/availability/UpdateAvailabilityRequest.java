package com.example.shifter.dto.availability;

import com.example.shifter.model.availability.Availability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvailabilityRequest {
    @NotNull(message = "Day of week is required")
    private Availability.DayOfWeek dayOfWeek;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "End time is required")
    private String endTime;
}
