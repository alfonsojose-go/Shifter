package com.example.shifter.service.availability;

import com.example.shifter.dto.availability.CreateAvailabilityRequest.AvailabilitySlot;
import com.example.shifter.model.User;
import com.example.shifter.model.availability.Availability;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.availability.AvailabilityRepository;
import com.example.shifter.util.availability.TimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;

    // ========== CREATE / REPLACE ==========
    @Override
    @Transactional
    public void createAvailability(Long employeeId, List<AvailabilitySlot> slots) {
        log.info("Creating availability slots for employee ID: {}, slot count: {}",
                employeeId, slots == null ? "null" : slots.size());

        // 1. Validate employee exists
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

        // 2. If slots is empty, just clear existing
        if (slots == null || slots.isEmpty()) {
            deleteAllAvailabilitiesOfEmployee(employeeId);
            log.info("Cleared all availabilities for employee {}", employeeId);
            return;
        }

        // 3. Parse and create Availability entities
        List<Availability> newAvailabilities = parseAndValidateSlots(employee, slots);

        // 4. Check overlaps only within the new submitted batch
        checkForBatchOverlaps(newAvailabilities);

        // 5. Replace strategy: delete old, save new
        availabilityRepository.deleteByEmployeeId(employeeId);
        availabilityRepository.saveAll(newAvailabilities);

        log.info("Successfully created {} availability slots for employee ID: {}",
                newAvailabilities.size(), employeeId);
    }

    // ========== UPDATE SINGLE ==========
    @Override
    @Transactional
    public void updateAvailabilityById(Long availabilityId,
                                       Availability.DayOfWeek dayOfWeek,
                                       String startTimeStr,
                                       String endTimeStr) {

        log.info("Updating availability ID: {}, day: {}, time: {} - {}",
                availabilityId, dayOfWeek, startTimeStr, endTimeStr);

        // 1. Find existing
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability not found with ID: " + availabilityId));

        // 2. Parse and validate
        LocalTime startTime = TimeUtils.parseTime(startTimeStr);
        LocalTime endTime = TimeUtils.parseTime(endTimeStr);
        validateTimeRange(startTime, endTime);

        // 3. Check overlaps with OTHER slots
        List<Availability> overlappingSlots = availabilityRepository.findOverlappingAvailabilities(
                availability.getEmployee().getId(),
                dayOfWeek,
                startTime,
                endTime,
                availabilityId // Exclude current
        );

        if (!overlappingSlots.isEmpty()) {
            Availability overlap = overlappingSlots.get(0);
            throw new IllegalArgumentException(
                    String.format("Overlaps with existing %s slot: %s - %s",
                            overlap.getDayOfWeek(),
                            TimeUtils.formatTo12Hour(overlap.getStartTime()),
                            TimeUtils.formatTo12Hour(overlap.getEndTime())));
        }

        // 4. Update and save
        availability.setDayOfWeek(dayOfWeek);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availabilityRepository.save(availability);

        log.info("Availability updated successfully for ID: {}", availabilityId);
    }

    // ========== DELETE ==========
    @Override
    @Transactional
    public void deleteAvailabilityById(Long availabilityId) {
        log.info("Deleting availability with ID {}", availabilityId);

        if (!availabilityRepository.existsById(availabilityId)) {
            throw new IllegalArgumentException("Availability not found with ID: " + availabilityId);
        }

        availabilityRepository.deleteById(availabilityId);
        log.info("Availability {} deleted successfully", availabilityId);
    }

    @Override
    @Transactional
    public void deleteAllAvailabilitiesOfEmployee(Long employeeId) {
        log.info("Deleting all availabilities for employee {}", employeeId);

        if (!userRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found with ID: " + employeeId);
        }

        int deletedCount = availabilityRepository.deleteByEmployeeId(employeeId);
        log.info("Deleted {} availability slot(s) for employee {}", deletedCount, employeeId);
    }

    // ========== GET METHODS ==========
    @Override
    public List<Availability> getAllAvailabilities() {
        log.info("Fetching all availabilities");
        return availabilityRepository.findAll();
    }

    @Override
    public List<Availability> getAvailabilitiesByEmployee(Long employeeId) {
        log.info("Fetching all availabilities for employee ID: {}", employeeId);

        if (!userRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found with ID: " + employeeId);
        }

        return availabilityRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<Availability> getAvailabilitiesByEmployeeAndDay(Long employeeId,
                                                                Availability.DayOfWeek dayOfWeek) {
        log.info("Fetching availabilities for employee ID: {} on day: {}", employeeId, dayOfWeek);

        if (!userRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found with ID: " + employeeId);
        }

        return availabilityRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek);
    }

    @Override
    public Availability getAvailabilityById(Long availabilityId) {
        log.info("Fetching availability with ID: {}", availabilityId);
        return availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability not found with ID: " + availabilityId));
    }

    // ========== UTILITY METHODS ==========
    @Override
    public boolean hasAvailabilities(Long employeeId) {
        log.info("Checking if employee ID: {} has availabilities", employeeId);
        return availabilityRepository.countByEmployeeId(employeeId) > 0;
    }

    @Override
    public List<User> getAvailableEmployeesByDay(Availability.DayOfWeek dayOfWeek) {
        log.info("Fetching all employees available on day: {}", dayOfWeek);
        return availabilityRepository.findAvailableEmployeesByDay(dayOfWeek);
    }

    @Override
    public void validateTimeRange(String startTimeStr, String endTimeStr) {
        log.info("Validating time range: {} - {}", startTimeStr, endTimeStr);
        LocalTime startTime = TimeUtils.parseTime(startTimeStr);
        LocalTime endTime = TimeUtils.parseTime(endTimeStr);
        validateTimeRange(startTime, endTime);
    }

    @Override
    public LocalTime parseTimeString(String timeString) {
        return TimeUtils.parseTime(timeString);
    }

    // ========== PRIVATE HELPER METHODS ==========
    private List<Availability> parseAndValidateSlots(User employee, List<AvailabilitySlot> slots) {
        List<Availability> availabilities = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < slots.size(); i++) {
            AvailabilitySlot slot = slots.get(i);
            try {
                LocalTime startTime = TimeUtils.parseTime(slot.getStartTime());
                LocalTime endTime = TimeUtils.parseTime(slot.getEndTime());
                validateTimeRange(startTime, endTime);

                Availability availability = new Availability();
                availability.setEmployee(employee);
                availability.setDayOfWeek(slot.getDayOfWeek());
                availability.setStartTime(startTime);
                availability.setEndTime(endTime);
                availabilities.add(availability);

            } catch (IllegalArgumentException e) {
                errors.add(String.format("Slot %d (%s %s-%s): %s",
                        i + 1, slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(), e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Batch validation failed: " + String.join("; ", errors));
        }

        return availabilities;
    }

    private void checkForBatchOverlaps(List<Availability> newAvailabilities) {
        if (newAvailabilities == null || newAvailabilities.size() <= 1) {
            return;
        }

        // Group by employee and day for efficient checking
        Map<Long, Map<Availability.DayOfWeek, List<Availability>>> grouped = new HashMap<>();
        for (Availability avail : newAvailabilities) {
            grouped.computeIfAbsent(avail.getEmployee().getId(), k -> new HashMap<>())
                    .computeIfAbsent(avail.getDayOfWeek(), k -> new ArrayList<>())
                    .add(avail);
        }

        // Check overlaps within each group
        for (Map<Availability.DayOfWeek, List<Availability>> dayMap : grouped.values()) {
            for (List<Availability> daySlots : dayMap.values()) {
                if (daySlots.size() > 1) {
                    daySlots.sort(Comparator.comparing(Availability::getStartTime));

                    for (int i = 0; i < daySlots.size() - 1; i++) {
                        Availability current = daySlots.get(i);
                        Availability next = daySlots.get(i + 1);

                        if (timesOverlap(current.getStartTime(), current.getEndTime(),
                                next.getStartTime(), next.getEndTime())) {
                            throw new IllegalArgumentException(
                                    String.format("Overlap in new schedule: %s %s-%s overlaps with %s-%s",
                                            current.getDayOfWeek(),
                                            TimeUtils.formatTo12Hour(current.getStartTime()),
                                            TimeUtils.formatTo12Hour(current.getEndTime()),
                                            TimeUtils.formatTo12Hour(next.getStartTime()),
                                            TimeUtils.formatTo12Hour(next.getEndTime())
                                    )
                            );
                        }
                    }
                }
            }
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    String.format("End time (%s) must be after start time (%s)",
                            TimeUtils.formatTo12Hour(endTime),
                            TimeUtils.formatTo12Hour(startTime))
            );
        }

        // Optional: Minimum duration
        if (java.time.Duration.between(startTime, endTime).toMinutes() < 30) {
            throw new IllegalArgumentException("Minimum availability duration is 30 minutes");
        }
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1,
                                 LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}