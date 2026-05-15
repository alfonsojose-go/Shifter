package com.example.shifter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShowSqueduleDTO {

    private Long userId;
    private String employeeName;
    private String position;

    private String date;
    private String startTime;
    private String endTime;
}