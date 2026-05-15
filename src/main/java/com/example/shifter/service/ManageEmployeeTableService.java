package com.example.shifter.service;

import com.example.shifter.dto.EmployeeTableResponse;
import com.example.shifter.dto.UpdateEmployeeTableRequest;

import java.util.List;

public interface ManageEmployeeTableService {
    List<EmployeeTableResponse> getAllEmployees();

    EmployeeTableResponse getEmployeeByName(String name);

    void updateEmployee(Long userId, UpdateEmployeeTableRequest request);
}
