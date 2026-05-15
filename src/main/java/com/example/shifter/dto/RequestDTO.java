package com.example.shifter.dto;

import com.example.shifter.enums.RequestType;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * This DTO is for Shift change request
 */
@Getter
@Setter
public class RequestDTO {
    private Long userId;

    private RequestType type;

    // Single-day fields
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    // Multi-day leave
    private LocalDate startDate;
    private LocalDate endDate;

    // Swap request
    private Long swapWithUserId;

    private String reason;

}
