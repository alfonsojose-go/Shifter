package com.example.shifter.util;

import com.example.shifter.dto.ClockRecordReportDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class to generate CSV content for attendance reports.
 */
public class CsvReportGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String generateAttendanceCsv(List<ClockRecordReportDTO> report) {
        StringBuilder sb = new StringBuilder();

        sb.append("User ID,Name,Date,Scheduled Start,Scheduled End,Clock In,Clock Out,Status,Hours Worked,Late By (mins),Left Early By (mins),Hourly Wage,Scheduled Cost,Actual Cost\n");

        for (ClockRecordReportDTO dto : report) {
            sb.append(dto.getUserId()).append(",");
            sb.append("\"").append(dto.getUserName() != null ? dto.getUserName() : "").append("\",");
            sb.append(dto.getWorkDate() != null ? dto.getWorkDate().format(DATE) : "").append(",");

            // LocalTime formats perfectly with the HH:mm formatter
            sb.append(dto.getScheduledStartTime() != null ? dto.getScheduledStartTime().format(TIME) : "").append(",");
            sb.append(dto.getScheduledEndTime() != null ? dto.getScheduledEndTime().format(TIME) : "").append(",");

            sb.append(dto.getClockInTime() != null ? dto.getClockInTime().format(DATE_TIME) : "").append(",");
            sb.append(dto.getClockOutTime() != null ? dto.getClockOutTime().format(DATE_TIME) : "").append(",");

            // Update by Laarni: Handled the AttendanceStatus enum safely so it doesn't print the word "null" if empty
            sb.append(dto.getAttendanceStatus() != null ? dto.getAttendanceStatus().name() : "").append(",");

            sb.append(dto.getTotalWorked() != null ? String.format("%.2f", dto.getTotalWorked().toMinutes() / 60.0) : "").append(",");
            sb.append(dto.getLateBy() != null ? dto.getLateBy().toMinutes() : "").append(",");
            sb.append(dto.getLeftEarlyBy() != null ? dto.getLeftEarlyBy().toMinutes() : "").append(",");

            // Update by Laarni: Formatted the Double monetary fields to strictly 2 decimal places to avoid floating-point artifacts (e.g., 15.4999999) in the CSV
            sb.append(dto.getHourlyWage() != null ? String.format("%.2f", dto.getHourlyWage()) : "").append(",");
            sb.append(dto.getScheduledCost() != null ? String.format("%.2f", dto.getScheduledCost()) : "").append(",");
            sb.append(dto.getActualCost() != null ? String.format("%.2f", dto.getActualCost()) : "");

            sb.append("\n");
        }

        return sb.toString();
    }
}