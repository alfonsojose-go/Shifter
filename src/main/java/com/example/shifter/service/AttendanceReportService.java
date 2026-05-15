package com.example.shifter.service;

import com.example.shifter.dto.ClockRecordReportDTO;
import com.example.shifter.model.ClockRecord;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.model.User;
import com.example.shifter.enums.AttendanceStatus;
import com.example.shifter.repository.ClockRecordRepository;
import com.example.shifter.repository.ScheduledShiftRepository;
import com.example.shifter.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates weekly attendance reports for employees.
 */
@Service
@RequiredArgsConstructor
public class AttendanceReportService {

    private final ScheduledShiftRepository shiftRepository;
    private final ClockRecordRepository clockRecordRepository;
    private final UserRepository userRepository;   //not used

    /**
     * Weekly report for ALL employees.
     */
    public List<ClockRecordReportDTO> getWeeklyReport(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        /* --- LAARNI'S UPDATE: Delegate to chronological mapping helper --- */
        List<ScheduledShift> shifts = shiftRepository.findByDateBetween(weekStart, weekEnd);
        List<ClockRecord> records = clockRecordRepository.findByWorkDateBetween(weekStart, weekEnd);
        return processMappedShifts(shifts, records);
        /* ----------------------------------------------------------------- */

        /* --- ORIGINAL CODE (COMMENTED OUT) ---
        List<ScheduledShift> shifts = shiftRepository.findByDateBetween(weekStart, weekEnd);
        List<ClockRecord> records = clockRecordRepository.findByWorkDateBetween(weekStart, weekEnd);

        Map<Long, List<ClockRecord>> recordsByUser = records.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        List<ClockRecordReportDTO> report = new ArrayList<>();

        for (ScheduledShift shift : shifts) {
            Long userId = shift.getEmployee().getId();
            List<ClockRecord> userRecords = recordsByUser.getOrDefault(userId, List.of());

            ClockRecord matchingRecord = userRecords.stream()
                    .filter(r -> r.getWorkDate().equals(shift.getDate()))
                    .findFirst()   //encounter issues if there is a broken shift. It always refer to first clockin
                    .orElse(null);

            report.add(buildDTO(shift, matchingRecord));
        }

        return report;
        --- END ORIGINAL CODE --- */
    }

    /**
     * Weekly report for ONE employee.
     */
    public List<ClockRecordReportDTO> getWeeklyReportForEmployee(Long employeeId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        /* --- LAARNI'S UPDATE: Delegate to chronological mapping helper --- */
        List<ScheduledShift> shifts = shiftRepository.findByEmployeeIdAndDateBetween(employeeId, weekStart, weekEnd);
        List<ClockRecord> records = clockRecordRepository.findByUserIdAndWorkDateBetween(employeeId, weekStart, weekEnd);
        return processMappedShifts(shifts, records);
        /* ----------------------------------------------------------------- */

        /* --- ORIGINAL CODE (COMMENTED OUT) ---
        List<ScheduledShift> shifts = shiftRepository.findByEmployeeIdAndDateBetween(employeeId, weekStart, weekEnd);
        List<ClockRecord> records = clockRecordRepository.findByUserIdAndWorkDateBetween(employeeId, weekStart, weekEnd);

        Map<LocalDate, ClockRecord> recordMap = records.stream()
                .collect(Collectors.toMap(ClockRecord::getWorkDate, r -> r));

        List<ClockRecordReportDTO> report = new ArrayList<>();

        for (ScheduledShift shift : shifts) {
            ClockRecord record = recordMap.get(shift.getDate());
            report.add(buildDTO(shift, record));
        }

        return report;
        --- END ORIGINAL CODE --- */
    }


    /**
     * Helper method to pair multiple shifts and records on the same day chronologically.
     * Changes made by Laarni
     */
    private List<ClockRecordReportDTO> processMappedShifts(List<ScheduledShift> shifts, List<ClockRecord> records) {
        List<ClockRecordReportDTO> report = new ArrayList<>();

        // Group records by "UserId_Date"
        Map<String, List<ClockRecord>> recordsByKey = records.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId() + "_" + r.getWorkDate()));

        // Group shifts by "UserId_Date"
        Map<String, List<ScheduledShift>> shiftsByKey = shifts.stream()
                .collect(Collectors.groupingBy(s -> s.getEmployee().getId() + "_" + s.getDate()));

        // Process each day's shifts for each user
        for (Map.Entry<String, List<ScheduledShift>> entry : shiftsByKey.entrySet()) {
            String key = entry.getKey();
            List<ScheduledShift> dayShifts = entry.getValue();
            List<ClockRecord> dayRecords = recordsByKey.getOrDefault(key, new ArrayList<>());

            // Sort both chronologically to handle broken/split shifts!
            // e.g. Morning Shift pairs with Morning Record. Evening Shift pairs with Evening Record.
            dayShifts.sort(Comparator.comparing(ScheduledShift::getStartTime));
            dayRecords.sort(Comparator.comparing(r -> r.getClockInTime() != null ? r.getClockInTime() : LocalDateTime.MAX));

            // Pair them up 1-to-1 in order
            for (int i = 0; i < dayShifts.size(); i++) {
                ScheduledShift shift = dayShifts.get(i);
                ClockRecord record = (i < dayRecords.size()) ? dayRecords.get(i) : null;
                report.add(buildDTO(shift, record));
            }
        }

        return report;
    }


    /**
     * Builds a DTO for a single shift. **Assumes shift is NEVER null.
     */
    private ClockRecordReportDTO buildDTO(ScheduledShift shift, ClockRecord record) {
        User user = shift.getEmployee();

        ClockRecordReportDTO dto = new ClockRecordReportDTO();
        dto.setUserId(user.getId());
        dto.setUserName(user.getFullName());
        dto.setWorkDate(shift.getDate());

        dto.setScheduledStartTime(shift.getStartTime());
        dto.setScheduledEndTime(shift.getEndTime());

        // Hourly wage
        BigDecimal wage = user.getPosition() != null ? user.getPosition().getHourlyWage() : BigDecimal.ZERO;
        double hourlyWage = wage != null ? wage.doubleValue() : 0.0;
        dto.setHourlyWage(hourlyWage);

        // Scheduled cost
        double scheduledHours = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes() / 60.0;
        dto.setScheduledCost(scheduledHours * hourlyWage);

        if (record == null) {
            dto.setAttendanceStatus(AttendanceStatus.ABSENT);
            dto.setActualCost(0.0);
            return dto;
        }

        dto.setClockInTime(record.getClockInTime());
        dto.setClockOutTime(record.getClockOutTime());

        // Actual hours
        if (record.getClockOutTime() != null) {
            double actualHours = Duration.between(record.getClockInTime(), record.getClockOutTime()).toMinutes() / 60.0;
            dto.setActualCost(actualHours * hourlyWage);
            dto.setTotalWorked(Duration.between(record.getClockInTime(), record.getClockOutTime()));
        } else {
            dto.setAttendanceStatus(AttendanceStatus.NO_CLOCK_OUT);
            dto.setActualCost(0.0);
            return dto;
        }

        // Late?
        if (record.getClockInTime().toLocalTime().isAfter(shift.getStartTime())) {
            dto.setLateBy(Duration.between(shift.getStartTime(), record.getClockInTime().toLocalTime()));
            dto.setAttendanceStatus(AttendanceStatus.LATE);
        }

        // Early leave?
        if (record.getClockOutTime().toLocalTime().isBefore(shift.getEndTime())) {
            dto.setLeftEarlyBy(Duration.between(record.getClockOutTime().toLocalTime(), shift.getEndTime()));
//            dto.setAttendanceStatus(AttendanceStatus.EARLY);

            // --- Added by Laarni -FIX: Only overwrite to EARLY if they weren't already marked LATE ---
            if (dto.getAttendanceStatus() != AttendanceStatus.LATE) {
                dto.setAttendanceStatus(AttendanceStatus.EARLY);
            }
            //END
        }

        // On time?
        if (dto.getAttendanceStatus() == null) {
            dto.setAttendanceStatus(AttendanceStatus.ON_TIME);
        }

        return dto;
    }
}