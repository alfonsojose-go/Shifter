package com.example.shifter.util.availability;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class TimeUtils {
    
    private static final DateTimeFormatter[] TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("h:mm a").withLocale(Locale.US),
        DateTimeFormatter.ofPattern("hh:mm a").withLocale(Locale.US),
        DateTimeFormatter.ofPattern("h:mm:ss a").withLocale(Locale.US), // Added
        DateTimeFormatter.ofPattern("hh:mm:ss a").withLocale(Locale.US), // Added
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm:ss")
    };

    private static final DateTimeFormatter FORMATTER_12H = 
        DateTimeFormatter.ofPattern("h:mm a").withLocale(Locale.US);
    
    private static final DateTimeFormatter FORMATTER_24H = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be null or empty");
        }
        
        String trimmedTime = timeStr.trim().toUpperCase()
            .replace("A.M.", "AM")
            .replace("P.M.", "PM")
            .replace(".", "");

         trimmedTime = normalizeTimeSeparators(trimmedTime);
        
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(trimmedTime, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        
        throw new IllegalArgumentException(
            "Cannot parse time: '" + timeStr + 
            "'. Supported formats: HH:mm, H:mm, HH:mm:ss, H:mm:ss, h:mm AM/PM, hh:mm AM/PM, h:mm:ss AM/PM, hh:mm:ss AM/PM");
    }

    private static String normalizeTimeSeparators(String timeStr) {
        // Remove all dots first (already done)
        // Now we need to convert "230" to "2:30" or "23015" to "2:30:15"
        
        // Check if it's a 12-hour format with AM/PM
        boolean hasAmPm = timeStr.contains("AM") || timeStr.contains("PM");
        String timePart = hasAmPm ? 
            timeStr.replace("AM", "").replace("PM", "").trim() : 
            timeStr;
        
        // Remove any remaining spaces
        timePart = timePart.replaceAll("\\s+", "");
        
        // If the string contains only digits (and possibly a colon already)
        if (timePart.matches("\\d+")) {
            int length = timePart.length();
            
            if (length <= 2) {
                // Just hours: "2" -> "2:00"
                return timePart + ":00" + (hasAmPm ? " " + getAmPmSuffix(timeStr) : "");
            } else if (length == 3) {
                // "230" -> "2:30"
                return timePart.substring(0, 1) + ":" + timePart.substring(1) + 
                       (hasAmPm ? " " + getAmPmSuffix(timeStr) : "");
            } else if (length == 4) {
                // "0230" -> "02:30"
                return timePart.substring(0, 2) + ":" + timePart.substring(2) + 
                       (hasAmPm ? " " + getAmPmSuffix(timeStr) : "");
            } else if (length == 5) {
                // "23015" -> "2:30:15"
                return timePart.substring(0, 1) + ":" + timePart.substring(1, 3) + ":" + timePart.substring(3) + 
                       (hasAmPm ? " " + getAmPmSuffix(timeStr) : "");
            } else if (length == 6) {
                // "023015" -> "02:30:15"
                return timePart.substring(0, 2) + ":" + timePart.substring(2, 4) + ":" + timePart.substring(4) + 
                       (hasAmPm ? " " + getAmPmSuffix(timeStr) : "");
            }
        }
        
        // If it already has colons or other format, return as is
        return timeStr;
    }
    
    private static String getAmPmSuffix(String timeStr) {
        if (timeStr.contains("AM")) return "AM";
        if (timeStr.contains("PM")) return "PM";
        return "";
    }
    
    public static LocalTime parseTime(String timeStr, String formatPattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern)
                .withLocale(Locale.US);
            return LocalTime.parse(timeStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Cannot parse time: '" + timeStr + "' with format: " + formatPattern, e);
        }
    }
    
    public static boolean isValidTime(String timeStr) {
        try {
            parseTime(timeStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Format LocalTime to 12-hour format (e.g., "2:30 PM")
     */
    public static String formatTo12Hour(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(FORMATTER_12H);
    }
    
    /**
     * Format LocalTime to 24-hour format (e.g., "14:30")
     */
    public static String formatTo24Hour(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(FORMATTER_24H);
    }
    
    /**
     * Format LocalTime to 12-hour format (String version)
     */
    public static String formatTo12Hour(String timeStr) {
        LocalTime time = parseTime(timeStr);
        return formatTo12Hour(time);
    }
    
    /**
     * Format LocalTime to 24-hour format (String version)
     */
    public static String formatTo24Hour(String timeStr) {
        LocalTime time = parseTime(timeStr);
        return formatTo24Hour(time);
    }
    
    /**
     * Normalize a time string to 24-hour format
     * Example: "2:30 PM" -> "14:30"
     */
    public static String normalizeTo24Hour(String timeStr) {
        LocalTime time = parseTime(timeStr);
        return formatTo24Hour(time);
    }
    
    /**
     * Check if two time ranges overlap
     */
    public static boolean doTimeRangesOverlap(LocalTime start1, LocalTime end1,
                                              LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
    
    /**
     * Check if two time ranges overlap (string version)
     */
    public static boolean doTimeRangesOverlap(String start1, String end1,
                                              String start2, String end2) {
        LocalTime s1 = parseTime(start1);
        LocalTime e1 = parseTime(end1);
        LocalTime s2 = parseTime(start2);
        LocalTime e2 = parseTime(end2);
        return doTimeRangesOverlap(s1, e1, s2, e2);
    }
}
