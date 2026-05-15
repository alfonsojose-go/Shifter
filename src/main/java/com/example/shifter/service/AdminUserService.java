package com.example.shifter.service;

import com.example.shifter.dto.CreateUserRequest;
import com.example.shifter.dto.UpdateUserRequest;
import com.example.shifter.dto.UserResponse;

import java.util.List;

public interface AdminUserService {
    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
