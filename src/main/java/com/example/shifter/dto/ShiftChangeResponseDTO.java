package com.example.shifter.dto;

import com.example.shifter.enums.RequestStatus;
import com.example.shifter.enums.RequestType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftChangeResponseDTO {

    private Long id;

    private Long userId;
    private String employeeName;

    private RequestType type;
    private RequestStatus status;

    // Single-day request
    private String date;
    private String startTime;
    private String endTime;

    // Multi-day leave
    private String startDate;
    private String endDate;

    // Swap request
    private Long swapWithUserId;

    private String reason;

    private String dayOfWeek;
}