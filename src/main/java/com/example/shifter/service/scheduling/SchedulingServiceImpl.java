package com.example.shifter.service.scheduling;

import com.example.shifter.dto.scheduling.CreateSchedulingRequest.SchedulingSlot;
import com.example.shifter.model.User;
import com.example.shifter.model.scheduling.Scheduling;
import com.example.shifter.repository.UserRepository;
import com.example.shifter.repository.scheduling.SchedulingRepository;
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
public class SchedulingServiceImpl implements SchedulingService {

    private final SchedulingRepository schedulingRepository;
    private final UserRepository userRepository;

    // ========== CREATE / REPLACE ==========
    @Override
    @Transactional
    public void createSchedule(Long employeeId, List<SchedulingSlot> slots) {
        log.info("Creating scheduling slots for employee ID: {}, slot count: {}",
                employeeId, slots == null ? "null" : slots.size());

        // 1. Validate employee exists
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

        // 2. If slots is empty, just clear existing
        if (slots == null || slots.isEmpty()) {
            deleteAllScheduleOfEmployee(employeeId);
            log.info("Cleared all availabilities for employee {}", employeeId);
            return;
        }

        // 3. Parse and create Scheduling entities
        List<Scheduling> newScheduling = parseAndValidateSlots(employee, slots);

        // 4. Check overlaps within new batch
        checkForBatchOverlaps(newScheduling);

        // 5. Check overlaps with existing slots (using repository)
        checkNewSlotsAgainstExisting(employeeId, newScheduling);

        // 6. Replace strategy: delete old, save new
        schedulingRepository.deleteByEmployeeId(employeeId);
        schedulingRepository.saveAll(newScheduling);
        
        log.info("Successfully created {} scheduling slots for employee ID: {}",
                newScheduling.size(), employeeId);
    }

    // ========== UPDATE SINGLE ==========
    @Override
    @Transactional
    public void updateScheduleById(Long schedulingId,
            Scheduling.DayOfWeek dayOfWeek,
            String startTimeStr,
            String endTimeStr) {

        log.info("Updating scheduling ID: {}, day: {}, time: {} - {}",
                schedulingId, dayOfWeek, startTimeStr, endTimeStr);

        // 1. Find existing
        Scheduling scheduling = schedulingRepository.findById(schedulingId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduling not found with ID: " + schedulingId));

        // 2. Parse and validate
        LocalTime startTime = TimeUtils.parseTime(startTimeStr);
        LocalTime endTime = TimeUtils.parseTime(endTimeStr);
        validateTimeRange(startTime, endTime);

        // 3. Check overlaps with OTHER slots
        List<Scheduling> overlappingSlots = schedulingRepository.findOverlappingSchedules(
                scheduling.getEmployee().getId(),
                dayOfWeek,
                startTime,
                endTime,
                schedulingId // Exclude current
        );

        if (!overlappingSlots.isEmpty()) {
            Scheduling overlap = overlappingSlots.get(0);
            throw new IllegalArgumentException(
                    String.format("Overlaps with existing %s slot: %s - %s",
                            overlap.getDayOfWeek(),
                            TimeUtils.formatTo12Hour(overlap.getStartTime()),
                            TimeUtils.formatTo12Hour(overlap.getEndTime())));
        }

        // 4. Update and save
        scheduling.setDayOfWeek(dayOfWeek);
        scheduling.setStartTime(startTime);
        scheduling.setEndTime(endTime);
        schedulingRepository.save(scheduling);
        
        log.info("Scheduling updated successfully for ID: {}", schedulingId);
    }

    // ========== DELETE ==========
    @Override
    @Transactional
    public void deleteScheduleById(Long schedulingId) {
        log.info("Deleting scheduling with ID {}", schedulingId);

        if (!schedulingRepository.existsById(schedulingId)) {
            throw new IllegalArgumentException("Scheduling not found with ID: " + schedulingId);
        }

        schedulingRepository.deleteById(schedulingId);
        log.info("Scheduling {} deleted successfully", schedulingId);
    }

    @Override
    @Transactional
    public void deleteAllScheduleOfEmployee(Long employeeId) {
        log.info("Deleting all availabilities for employee {}", employeeId);

        if (!userRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found with ID: " + employeeId);
        }

        int deletedCount = schedulingRepository.deleteByEmployeeId(employeeId);
        log.info("Deleted {} scheduling slot(s) for employee {}", deletedCount, employeeId);
    }

    // ========== GET METHODS ==========
    @Override
    public List<Scheduling> getAllSchedules() {
        log.info("Fetching all availabilities");
        return schedulingRepository.findAll();
    }

    @Override
    public List<Scheduling> getSchedulesByEmployee(Long employeeId) {
        log.info("Fetching all availabilities for employee ID: {}", employeeId);

        if (!userRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found with ID: " + employeeId);
        }

        return schedulingRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<Scheduling> getSchedulesByEmployeeAndDay(Long employeeId,
            Scheduling.DayOfWeek dayOfWeek) {
        log.info("Fetching schedules for employee ID: {} on day: {}", employeeId, dayOfWeek);

        if (!userRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found with ID: " + employeeId);
        }

        return schedulingRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek);
    }

    @Override
    public Scheduling getScheduleById(Long schedulingId) {
        log.info("Fetching scheduling with ID: {}", schedulingId);
        return schedulingRepository.findById(schedulingId)
                .orElseThrow(() -> new IllegalArgumentException("Scheduling not found with ID: " + schedulingId));
    }

    // ========== UTILITY METHODS ==========
    @Override
    public boolean hasSchedule(Long employeeId) {
        log.info("Checking if employee ID: {} has schedules", employeeId);
        return schedulingRepository.countByEmployeeId(employeeId) > 0;
    }

    @Override
    public List<User> getAvailableEmployeesByDay(Scheduling.DayOfWeek dayOfWeek) {
        log.info("Fetching all employees schedules on day: {}", dayOfWeek);
        return schedulingRepository.findAvailableEmployeesByDay(dayOfWeek);
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
    private List<Scheduling> parseAndValidateSlots(User employee, List<SchedulingSlot> slots) {
        List<Scheduling> availabilities = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < slots.size(); i++) {
            SchedulingSlot slot = slots.get(i);
            try {
                LocalTime startTime = TimeUtils.parseTime(slot.getStartTime());
                LocalTime endTime = TimeUtils.parseTime(slot.getEndTime());
                validateTimeRange(startTime, endTime);

                Scheduling scheduling = new Scheduling();
                scheduling.setEmployee(employee);
                scheduling.setDayOfWeek(slot.getDayOfWeek());
                scheduling.setStartTime(startTime);
                scheduling.setEndTime(endTime);
                availabilities.add(scheduling);

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

    private void checkForBatchOverlaps(List<Scheduling> newScheduling) {
        if (newScheduling == null || newScheduling.size() <= 1) {
            return;
        }

        // Group by employee and day for efficient checking
        Map<Long, Map<Scheduling.DayOfWeek, List<Scheduling>>> grouped = new HashMap<>();
        for (Scheduling avail : newScheduling) {
            grouped.computeIfAbsent(avail.getEmployee().getId(), k -> new HashMap<>())
                   .computeIfAbsent(avail.getDayOfWeek(), k -> new ArrayList<>())
                   .add(avail);
        }

        // Check overlaps within each group
        for (Map<Scheduling.DayOfWeek, List<Scheduling>> dayMap : grouped.values()) {
            for (List<Scheduling> daySlots : dayMap.values()) {
                if (daySlots.size() > 1) {
                    daySlots.sort(Comparator.comparing(Scheduling::getStartTime));
                    
                    for (int i = 0; i < daySlots.size() - 1; i++) {
                        Scheduling current = daySlots.get(i);
                        Scheduling next = daySlots.get(i + 1);
                        
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

    private void checkNewSlotsAgainstExisting(Long employeeId, List<Scheduling> newSlots) {
        for (Scheduling newSlot : newSlots) {
            List<Scheduling> existingOverlaps = schedulingRepository.findOverlappingSchedules(
                    employeeId,
                    newSlot.getDayOfWeek(),
                    newSlot.getStartTime(),
                    newSlot.getEndTime(),
                    null // No exclusion for new slots
            );

            if (!existingOverlaps.isEmpty()) {
                Scheduling conflictingSlot = existingOverlaps.get(0);
                throw new IllegalArgumentException(
                    String.format("Overlaps with existing %s slot: %s - %s",
                        conflictingSlot.getDayOfWeek(),
                        TimeUtils.formatTo12Hour(conflictingSlot.getStartTime()),
                        TimeUtils.formatTo12Hour(conflictingSlot.getEndTime())
                    )
                );
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
            throw new IllegalArgumentException("Minimum scheduling duration is 30 minutes");
        }
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1,
                                 LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}