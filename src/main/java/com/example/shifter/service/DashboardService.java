package com.example.shifter.service;

import com.example.shifter.dto.EmployeeRecentRequestDTO;
import com.example.shifter.dto.ManagerOldestRequestDTO;
import com.example.shifter.enums.RequestStatus;
import com.example.shifter.model.Request;
import com.example.shifter.model.User;
import com.example.shifter.repository.RequestRepository;
import com.example.shifter.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;

    // ---------------- EMPLOYEE ----------------

    public long getEmployeePendingCount() {
        User loggedUser = getLoggedUser();
        return requestRepository.countByUserIdAndStatus(loggedUser.getId(), RequestStatus.PENDING);
    }

    public List<EmployeeRecentRequestDTO> getEmployeeRecentRequests() {
        User loggedUser = getLoggedUser();
        return requestRepository.findTop5ByUserIdOrderByIdDesc(loggedUser.getId())
                .stream()
                .map(req -> new EmployeeRecentRequestDTO(
                        req.getType(),
                        req.getStatus(),
                        req.getDate() != null ? req.getDate() : req.getStartDate()
                ))
                .toList();
    }

    // ---------------- MANAGER ----------------

    public long getActiveEmployeesCount() {
        return userRepository.countUsersByRoleName("EMPLOYEE");
    }

    public long getPendingRequestsCount() {
        return requestRepository.countByStatus(RequestStatus.PENDING);
    }

    public List<ManagerOldestRequestDTO> getOldestPendingRequests() {
        return requestRepository.findTop3ByStatusOrderByIdAsc(RequestStatus.PENDING)
                .stream()
                .map(req -> new ManagerOldestRequestDTO(
                        req.getId(),
                        req.getUser().getFullName(),
                        req.getType(),
                        req.getDate() != null ? req.getDate() : req.getStartDate()
                ))
                .toList();
    }

    // ---------------- UTIL ----------------

    private User getLoggedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Logged user not found"));
    }
}