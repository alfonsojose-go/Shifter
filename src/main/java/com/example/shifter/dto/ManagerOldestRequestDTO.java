package com.example.shifter.dto;

import com.example.shifter.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * This Dto is for returning the pending requests for the manager
 */
@Getter
@AllArgsConstructor
public class ManagerOldestRequestDTO {
    private Long requestId;
    private String employeeName;
    private RequestType type;
    private LocalDate date;
}