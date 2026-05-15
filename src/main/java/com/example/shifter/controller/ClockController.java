package com.example.shifter.controller;

import com.example.shifter.dto.ClockRecordDTO;
import com.example.shifter.model.ClockRecord;
import com.example.shifter.service.ClockService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clock")
@RequiredArgsConstructor
public class ClockController {

    private final ClockService clockService;

    /** Employee clock-in */
    @PostMapping("/in")
    @PreAuthorize("hasAuthority('EMPLOYEE')")
    public ResponseEntity<ClockRecordDTO> clockIn() {
        return ResponseEntity.ok(clockService.clockIn());
    }

    /** Employee clock-out */
    @PostMapping("/out")
    @PreAuthorize("hasAuthority('EMPLOYEE')")
    public ResponseEntity<ClockRecordDTO> clockOut() {
        return ResponseEntity.ok(clockService.clockOut());
    }

    /** Check if employee is clocked-in */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('EMPLOYEE')")
    public ResponseEntity<?> status() {
        boolean isClockedIn = clockService.isClockedIn();
        return ResponseEntity.ok(
                isClockedIn ? "You are clocked-in" : "You are clocked-out"
        );
    }
}