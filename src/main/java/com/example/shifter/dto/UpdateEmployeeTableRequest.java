package com.example.shifter.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;



@Getter
@Setter
@Data
public class UpdateEmployeeTableRequest {
//    private Long positionId;
//    private Long contractTypeId;
//    private Set<Long> skillIds;

    // Replaced Long IDs with Strings to allow free-typing
    private String positionName;
    private String contractTypeName;

    // Replaced Set<Long> with a single comma-separated String
    private String skillsCsv;

    // Added hourly wage so the manager can update their pay!
    private BigDecimal hourlyWage;
}
