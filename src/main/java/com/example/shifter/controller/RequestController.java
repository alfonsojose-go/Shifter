package com.example.shifter.controller;

import com.example.shifter.dto.RequestDTO;
import com.example.shifter.dto.ShiftChangeResponseDTO;
import com.example.shifter.enums.RequestStatus;
import com.example.shifter.service.RequestService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for shift change and availability requests.
 * Uses:
 * - RequestDTO for incoming JSON (POST body)
 * - ShiftChangeResponseDTO for outgoing JSON (API responses)
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    /**
     * Employees + Managers can create requests.
     * INPUT: RequestDTO
     * OUTPUT: ShiftChangeResponseDTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<ShiftChangeResponseDTO> createRequest(@RequestBody RequestDTO dto) {
        return ResponseEntity.ok(requestService.createRequest(dto));
    }

    /**
     * Managers can approve or reject requests.
     * OUTPUT: ShiftChangeResponseDTO
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ShiftChangeResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam RequestStatus status
    ) {
        return ResponseEntity.ok(requestService.updateStatus(id, status));
    }

    /**
     * Managers can view all requests.
     * OUTPUT: List<ShiftChangeResponseDTO>
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ShiftChangeResponseDTO>> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    /**
     * Employees can view their own requests.
     * Managers can view any user's requests.
     * OUTPUT: List<ShiftChangeResponseDTO>
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    public ResponseEntity<List<ShiftChangeResponseDTO>> getRequestsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(requestService.getRequestsByUser(userId));
    }
}