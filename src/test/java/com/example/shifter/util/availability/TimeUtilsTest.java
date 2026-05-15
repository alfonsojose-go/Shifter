package com.example.shifter.util.availability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TimeUtilsTest {
    
    @Nested
    @DisplayName("parseTime tests")
    class ParseTimeTests {
        
        @ParameterizedTest
        @MethodSource("provideValidTimeStrings")
        @DisplayName("Should parse valid time strings")
        void parseTime_ValidInput_ReturnsLocalTime(String timeString, LocalTime expected) {
            LocalTime result = TimeUtils.parseTime(timeString);
            assertEquals(expected, result);
        }
        
        private static Stream<Arguments> provideValidTimeStrings() {
            return Stream.of(
                arguments("2:30 PM", LocalTime.of(14, 30)),
                arguments("02:30 PM", LocalTime.of(14, 30)),
                arguments("14:30", LocalTime.of(14, 30)),
                arguments("2:30", LocalTime.of(2, 30)),
                arguments("14:30:15", LocalTime.of(14, 30, 15)),
                arguments("2:30:15 PM", LocalTime.of(14, 30, 15)),
                arguments(" 2:30 PM ", LocalTime.of(14, 30)), // with spaces
                arguments("2:30 pm", LocalTime.of(14, 30)), // lowercase
                arguments("2.30 P.M.", LocalTime.of(14, 30)), // with periods
                arguments("2.30 A.M.", LocalTime.of(2, 30)), // AM with periods
                arguments("12:00 AM", LocalTime.of(0, 0)), // midnight
                arguments("12:00 PM", LocalTime.of(12, 0)), // noon
                arguments("00:00", LocalTime.of(0, 0)), // 24h midnight
                arguments("23:59", LocalTime.of(23, 59)), // end of day
                arguments("11:59 PM", LocalTime.of(23, 59)) // end of day 12h
            );
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("Should throw exception for null or empty input")
        void parseTime_NullOrEmpty_ThrowsException(String timeString) {
            assertThrows(IllegalArgumentException.class, 
                () -> TimeUtils.parseTime(timeString));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "25:00",
            "14:60",
            "2:30 XM",
            "invalid",
            "14:30:99",
            "99:99"
        })
        @DisplayName("Should throw exception for invalid time strings")
        void parseTime_InvalidInput_ThrowsException(String timeString) {
            assertThrows(IllegalArgumentException.class, 
                () -> TimeUtils.parseTime(timeString));
        }
        
        @Test
        @DisplayName("Should parse time with custom format")
        void parseTime_WithCustomFormat_ReturnsLocalTime() {
            LocalTime result = TimeUtils.parseTime("14-30", "HH-mm");
            assertEquals(LocalTime.of(14, 30), result);
        }
        
        @Test
        @DisplayName("Should throw exception for invalid custom format")
        void parseTime_InvalidCustomFormat_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> TimeUtils.parseTime("14:30", "HH-mm"));
        }
    }
    
    @Nested
    @DisplayName("isValidTime tests")
    class IsValidTimeTests {
        
        @ParameterizedTest
        @ValueSource(strings = {
            "2:30 PM",
            "14:30",
            "23:59",
            "12:00 AM"
        })
        @DisplayName("Should return true for valid time strings")
        void isValidTime_ValidInput_ReturnsTrue(String timeString) {
            assertTrue(TimeUtils.isValidTime(timeString));
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "invalid",
            "25:00",
            "14:60",
            "99:99"
        })
        @DisplayName("Should return false for invalid time strings")
        void isValidTime_InvalidInput_ReturnsFalse(String timeString) {
            assertFalse(TimeUtils.isValidTime(timeString));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return false for null or empty input")
        void isValidTime_NullOrEmpty_ReturnsFalse(String timeString) {
            assertFalse(TimeUtils.isValidTime(timeString));
        }
    }
    
    @Nested
    @DisplayName("formatTo12Hour tests")
    class FormatTo12HourTests {
        
        @Test
        @DisplayName("Should format LocalTime to 12-hour format")
        void formatTo12Hour_LocalTime_ReturnsFormattedString() {
            assertEquals("2:30 PM", TimeUtils.formatTo12Hour(LocalTime.of(14, 30)));
            assertEquals("12:00 AM", TimeUtils.formatTo12Hour(LocalTime.of(0, 0)));
            assertEquals("12:00 PM", TimeUtils.formatTo12Hour(LocalTime.of(12, 0)));
            assertEquals("11:59 PM", TimeUtils.formatTo12Hour(LocalTime.of(23, 59)));
            assertEquals("1:05 AM", TimeUtils.formatTo12Hour(LocalTime.of(1, 5)));
        }
        
        @Test
        @DisplayName("Should return null for null input")
        void formatTo12Hour_NullLocalTime_ReturnsNull() {
            assertNull(TimeUtils.formatTo12Hour((LocalTime) null));
        }
        
        @Test
        @DisplayName("Should format string time to 12-hour format")
        void formatTo12Hour_String_ReturnsFormattedString() {
            assertEquals("2:30 PM", TimeUtils.formatTo12Hour("14:30"));
            assertEquals("2:30 PM", TimeUtils.formatTo12Hour("2:30 PM"));
            assertEquals("12:00 AM", TimeUtils.formatTo12Hour("00:00"));
        }
    }
    
    @Nested
    @DisplayName("formatTo24Hour tests")
    class FormatTo24HourTests {
        
        @Test
        @DisplayName("Should format LocalTime to 24-hour format")
        void formatTo24Hour_LocalTime_ReturnsFormattedString() {
            assertEquals("14:30", TimeUtils.formatTo24Hour(LocalTime.of(14, 30)));
            assertEquals("00:00", TimeUtils.formatTo24Hour(LocalTime.of(0, 0)));
            assertEquals("12:00", TimeUtils.formatTo24Hour(LocalTime.of(12, 0)));
            assertEquals("23:59", TimeUtils.formatTo24Hour(LocalTime.of(23, 59)));
            assertEquals("01:05", TimeUtils.formatTo24Hour(LocalTime.of(1, 5)));
        }
        
        @Test
        @DisplayName("Should return null for null input")
        void formatTo24Hour_NullLocalTime_ReturnsNull() {
            assertNull(TimeUtils.formatTo24Hour((LocalTime) null));
        }
        
        @Test
        @DisplayName("Should format string time to 24-hour format")
        void formatTo24Hour_String_ReturnsFormattedString() {
            assertEquals("14:30", TimeUtils.formatTo24Hour("2:30 PM"));
            assertEquals("14:30", TimeUtils.formatTo24Hour("14:30"));
            assertEquals("00:00", TimeUtils.formatTo24Hour("12:00 AM"));
        }
    }
    
    @Nested
    @DisplayName("normalizeTo24Hour tests")
    class NormalizeTo24HourTests {
        
        @ParameterizedTest
        @CsvSource({
            "2:30 PM, 14:30",
            "12:00 AM, 00:00",
            "12:00 PM, 12:00",
            "11:59 PM, 23:59",
            "1:05 AM, 01:05",
            "14:30, 14:30"
        })
        @DisplayName("Should normalize various time formats to 24-hour format")
        void normalizeTo24Hour_ValidInput_Returns24HourFormat(String input, String expected) {
            assertEquals(expected, TimeUtils.normalizeTo24Hour(input));
        }
    }
    
    @Nested
    @DisplayName("doTimeRangesOverlap tests")
    class DoTimeRangesOverlapTests {
        
        @ParameterizedTest
        @MethodSource("provideOverlapTestCases")
        @DisplayName("Should correctly determine if time ranges overlap")
        void doTimeRangesOverlap_VariousRanges_ReturnsCorrectResult(
            LocalTime start1, LocalTime end1,
            LocalTime start2, LocalTime end2,
            boolean expected) {
            assertEquals(expected, 
                TimeUtils.doTimeRangesOverlap(start1, end1, start2, end2));
        }
        
        private static Stream<Arguments> provideOverlapTestCases() {
            return Stream.of(
                // Overlapping cases
                arguments(LocalTime.of(9, 0), LocalTime.of(17, 0), 
                         LocalTime.of(13, 0), LocalTime.of(15, 0), true),
                arguments(LocalTime.of(13, 0), LocalTime.of(15, 0),
                         LocalTime.of(9, 0), LocalTime.of(17, 0), true),
                arguments(LocalTime.of(9, 0), LocalTime.of(12, 0),
                         LocalTime.of(10, 0), LocalTime.of(11, 0), true),
                arguments(LocalTime.of(9, 0), LocalTime.of(12, 0),
                         LocalTime.of(11, 0), LocalTime.of(14, 0), true),
                
                // Non-overlapping cases
                arguments(LocalTime.of(9, 0), LocalTime.of(12, 0),
                         LocalTime.of(13, 0), LocalTime.of(17, 0), false),
                arguments(LocalTime.of(13, 0), LocalTime.of(17, 0),
                         LocalTime.of(9, 0), LocalTime.of(12, 0), false),
                
                // Edge cases (touching but not overlapping)
                arguments(LocalTime.of(9, 0), LocalTime.of(12, 0),
                         LocalTime.of(12, 0), LocalTime.of(14, 0), false),
                arguments(LocalTime.of(12, 0), LocalTime.of(14, 0),
                         LocalTime.of(9, 0), LocalTime.of(12, 0), false)
            );
        }
        
        @ParameterizedTest
        @MethodSource("provideOverlapStringTestCases")
        @DisplayName("Should correctly determine if time ranges overlap (string version)")
        void doTimeRangesOverlap_StringVersion_VariousRanges_ReturnsCorrectResult(
            String start1, String end1,
            String start2, String end2,
            boolean expected) {
            assertEquals(expected, 
                TimeUtils.doTimeRangesOverlap(start1, end1, start2, end2));
        }
        
        private static Stream<Arguments> provideOverlapStringTestCases() {
            return Stream.of(
                arguments("9:00 AM", "5:00 PM", "1:00 PM", "3:00 PM", true),
                arguments("9:00 AM", "12:00 PM", "1:00 PM", "5:00 PM", false),
                arguments("9:00", "12:00", "11:00", "14:00", true),
                arguments("09:00", "12:00", "12:00", "14:00", false)
            );
        }
        
        @Test
        @DisplayName("Should throw exception for invalid time strings in overlap check")
        void doTimeRangesOverlap_InvalidStringInput_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                () -> TimeUtils.doTimeRangesOverlap(
                    "invalid", "12:00 PM", "1:00 PM", "3:00 PM"));
        }
    }
    
    @Test
    @DisplayName("Integration test: parse, format, and normalize")
    void integrationTest_ParseFormatNormalize() {
        // Parse 12-hour format
        LocalTime time = TimeUtils.parseTime("2:30 PM");
        assertEquals(LocalTime.of(14, 30), time);
        
        // Format to 12-hour
        String formatted12h = TimeUtils.formatTo12Hour(time);
        assertEquals("2:30 PM", formatted12h);
        
        // Format to 24-hour
        String formatted24h = TimeUtils.formatTo24Hour(time);
        assertEquals("14:30", formatted24h);
        
        // Normalize
        String normalized = TimeUtils.normalizeTo24Hour("2:30 PM");
        assertEquals("14:30", normalized);
        
        // Verify consistency
        assertEquals(formatted24h, normalized);
    }
    
    @Test
    @DisplayName("Test time range validation with various formats")
    void testTimeRangeValidationWithVariousFormats() {
        // Test with mixed formats
        assertTrue(TimeUtils.doTimeRangesOverlap(
            "9:00 AM", "5:00 PM", 
            "14:00", "16:00")); // 2:00 PM - 4:00 PM
        
        assertFalse(TimeUtils.doTimeRangesOverlap(
            "8:00 AM", "12:00 PM",
            "13:00", "17:00")); // 1:00 PM - 5:00 PM
    }
}