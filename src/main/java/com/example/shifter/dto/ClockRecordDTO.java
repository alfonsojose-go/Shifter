package com.example.shifter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * This Dto works with ClockRecord controller
 */
@Getter
@AllArgsConstructor
public class ClockRecordDTO {

    private Long id;
    private Long userId;
    private String userName;

    private LocalDate workDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;

    private boolean active;
}