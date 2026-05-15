package com.example.shifter.service;

import com.example.shifter.dto.ClockRecordDTO;
import com.example.shifter.model.User;
import com.example.shifter.model.ClockRecord;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.repository.ClockRecordRepository;
import com.example.shifter.repository.ScheduledShiftRepository;
import com.example.shifter.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * - business rules:
 * - A user cannot clock‑in twice without clocking out
 * - A user cannot clock‑out without clocking in
 * - System automatically captures timestamps
 * - Later we can compare with scheduled shifts
 * - Must have a shift today
 * - Can only clock‑in starting 5 minutes before shift
 */
@Service
@RequiredArgsConstructor
public class ClockService {

    private final ClockRecordRepository clockRecordRepository;
    private final UserRepository userRepository;
    private final ScheduledShiftRepository scheduledShiftRepository;

    /** Clock-in logic */
    public ClockRecordDTO clockIn() {
        User user = getLoggedUser();
        LocalDate today = LocalDate.now();

        // 1. Check if user is already clocked in today
        clockRecordRepository.findByUserIdAndWorkDateAndActive(user.getId(), today, true)
                .ifPresent(record -> {
                    throw new RuntimeException("You are already clocked-in.");
                });

        // 2. Check if user has a scheduled shift today
        List<ScheduledShift> shifts = scheduledShiftRepository
                .findByEmployeeIdAndDate(user.getId(), today);

        if (shifts.isEmpty()) {
            throw new RuntimeException("You cannot clock in because you do not have a scheduled shift today.");
        }

        ScheduledShift shift = shifts.get(0); // first shift of the day

        // 3. Check if current time is within allowed clock-in window (5 minutes before shift)
        LocalTime now = LocalTime.now();
        LocalTime allowedClockInTime = shift.getStartTime().minusMinutes(5);

        if (now.isBefore(allowedClockInTime)) {
            throw new RuntimeException(
                    "You can only clock in starting 5 minutes before your shift (" +
                            allowedClockInTime + ")."
            );
        }


        // 4. Create clock-in record
        ClockRecord record = new ClockRecord();
        record.setUser(user);
        record.setWorkDate(today);
        record.setClockInTime(LocalDateTime.now());
        record.setActive(true);

        ClockRecord saved = clockRecordRepository.save(record);

        return new ClockRecordDTO(
                saved.getId(),
                user.getId(),
                user.getFullName(),
                saved.getWorkDate(),
                saved.getClockInTime(),
                saved.getClockOutTime(),
                saved.isActive()
        );

    }

    /** Clock-out logic */
    public ClockRecordDTO clockOut() {
        User user = getLoggedUser();
        LocalDate today = LocalDate.now();

        // Must have an active clock-in
        ClockRecord record = clockRecordRepository
                .findByUserIdAndWorkDateAndActive(user.getId(), today, true)
                .orElseThrow(() -> new RuntimeException("You must clock-in before clocking out."));

        record.setClockOutTime(LocalDateTime.now());
        record.setActive(false);

        ClockRecord saved = clockRecordRepository.save(record);

        return new ClockRecordDTO(
                saved.getId(),
                user.getId(),
                user.getFullName(),
                saved.getWorkDate(),
                saved.getClockInTime(),
                saved.getClockOutTime(),
                saved.isActive()
        );

    }

    /** Check if user is currently clocked in */
    public boolean isClockedIn() {
        User user = getLoggedUser();
        LocalDate today = LocalDate.now();

        return clockRecordRepository
                .findByUserIdAndWorkDateAndActive(user.getId(), today, true)
                .isPresent();
    }

    /** Utility: get logged user */
    private User getLoggedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("AUTH OBJECT: " + auth);
        System.out.println("AUTH NAME: " + (auth != null ? auth.getName() : "null"));

        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("No authenticated user found in SecurityContext");
        }

        String username = auth.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Logged user not found: " + username));
    }
}