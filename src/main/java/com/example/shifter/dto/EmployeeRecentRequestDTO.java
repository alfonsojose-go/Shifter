package com.example.shifter.dto;

import com.example.shifter.enums.RequestStatus;
import com.example.shifter.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * This Dto is for returning information about employee recent requests
 */
@Getter
@AllArgsConstructor
public class EmployeeRecentRequestDTO {
    private RequestType type;
    private RequestStatus status;
    private LocalDate date;
}