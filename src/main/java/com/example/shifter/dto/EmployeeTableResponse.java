package com.example.shifter.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@Data
public class EmployeeTableResponse {
    private Long userId;
    private String fullName;
    private String email;  //Added by Laarni Cerna
    private String role; //Added by Laarni Cerna
    private String position;
    private String contractType;
    private Set<String> skills;
    private BigDecimal hourlyWage;
}
