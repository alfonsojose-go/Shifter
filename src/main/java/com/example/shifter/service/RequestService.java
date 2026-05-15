package com.example.shifter.service;

import com.example.shifter.dto.RequestDTO;
import com.example.shifter.dto.ShiftChangeResponseDTO;
import com.example.shifter.enums.RequestStatus;
import com.example.shifter.model.Request;
import com.example.shifter.model.User;
import com.example.shifter.model.AvailabilityException;
import com.example.shifter.model.ScheduledShift;
import com.example.shifter.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles all business logic for employee shift-change and availability requests.
 */
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ScheduledShiftRepository scheduledShiftRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;

    // -------------------------------------------------------------------------
    // CREATE REQUEST (VALIDATION MOVED HERE)
    // -------------------------------------------------------------------------
    public ShiftChangeResponseDTO createRequest(RequestDTO dto) {

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Request request = new Request();
        request.setUser(user);
        request.setType(dto.getType());
        request.setDate(dto.getDate());
        request.setStartTime(dto.getStartTime());
        request.setEndTime(dto.getEndTime());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setReason(dto.getReason());
        request.setStatus(RequestStatus.PENDING);

        // Load swap partner if provided
        if (dto.getSwapWithUserId() != null) {
            User swapUser = userRepository.findById(dto.getSwapWithUserId())
                    .orElseThrow(() -> new RuntimeException("Swap user not found"));
            request.setSwapWithUser(swapUser);
        }

        // -------------------------------
        // SHIFT SWAP VALIDATION HERE
        // -------------------------------
        if (dto.getType().name().equals("SHIFT_SWAP")) {

            Long requesterId = user.getId();
            LocalDate date = dto.getDate();

            // Requester MUST have a shift
            List<ScheduledShift> requesterShifts =
                    scheduledShiftRepository.findByEmployeeIdAndDate(requesterId, date);

            if (requesterShifts.isEmpty()) {
                throw new RuntimeException(
                        "You cannot request a swap because you do not have a shift on this date."
                );
            }

            // Swap partner MAY or MAY NOT have a shift — both are valid
        }

        Request saved = requestRepository.save(request);
        return toDTO(saved);
    }

    // -------------------------------------------------------------------------
    // GET REQUESTS
    // -------------------------------------------------------------------------
    public List<ShiftChangeResponseDTO> getAllRequests() {
        return requestRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ShiftChangeResponseDTO> getRequestsByUser(Long userId) {
        return requestRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // -------------------------------------------------------------------------
    // APPROVE / REJECT REQUEST
    // -------------------------------------------------------------------------
    public ShiftChangeResponseDTO updateStatus(Long id, RequestStatus status) {

        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Apply business logic only when approving
        if (status == RequestStatus.APPROVED) {
            applyBusinessLogic(request);
        }

        request.setStatus(status);
        Request saved = requestRepository.save(request);

        return toDTO(saved);
    }

    // -------------------------------------------------------------------------
    // BUSINESS LOGIC (APPLIED ONLY AFTER MANAGER APPROVAL)
    // -------------------------------------------------------------------------
    private void applyBusinessLogic(Request request) {

        switch (request.getType()) {

            case WANT_TO_WORK -> handleWantToWork(request);

            case UNAVAILABLE -> handleUnavailable(request);

            case SICK_DAY -> handleSickDay(request);

            case LEAVE_OF_ABSENCE -> handleLeaveOfAbsence(request);

            case SHIFT_SWAP -> handleShiftSwap(request);
        }
    }

    // WANT TO WORK → add scheduled shift
    private void handleWantToWork(Request request) {
        ScheduledShift shift = new ScheduledShift(
                request.getUser(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        scheduledShiftRepository.save(shift);
    }

    // UNAVAILABLE → remove shift + add exception
    private void handleUnavailable(Request request) {

        removeScheduledShift(request.getUser().getId(), request.getDate());

        AvailabilityException ex = new AvailabilityException(
                request.getUser(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getReason()
        );
        availabilityExceptionRepository.save(ex);
    }

    // SICK DAY → remove shift + add exception
    private void handleSickDay(Request request) {
        removeScheduledShift(request.getUser().getId(), request.getDate());

        AvailabilityException ex = new AvailabilityException(
                request.getUser(),
                request.getDate(),
                null,
                null,
                "Sick Day"
        );

        availabilityExceptionRepository.save(ex);
    }

    // LEAVE OF ABSENCE → remove shifts for date range + add exceptions
    private void handleLeaveOfAbsence(Request request) {

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            removeScheduledShift(request.getUser().getId(), date);

            AvailabilityException ex = new AvailabilityException(
                    request.getUser(),
                    date,
                    null,
                    null,
                    request.getReason()
            );

            availabilityExceptionRepository.save(ex);
        }
    }

    // SHIFT SWAP → requester must have shift; partner may or may not
    private void handleShiftSwap(Request request) {

        Long userA = request.getUser().getId();          // requester
        Long userB = request.getSwapWithUser().getId();  // swap partner
        LocalDate date = request.getDate();

        List<ScheduledShift> shiftsA = scheduledShiftRepository.findByEmployeeIdAndDate(userA, date);
        ScheduledShift shiftA = shiftsA.get(0); // safe because validated earlier

        List<ScheduledShift> shiftsB = scheduledShiftRepository.findByEmployeeIdAndDate(userB, date);

        if (shiftsB.isEmpty()) {
            // Partner has no shift → give them A's shift
            shiftA.setEmployee(request.getSwapWithUser());
            scheduledShiftRepository.save(shiftA);
            return;
        }

        // Partner has a shift → swap them
        ScheduledShift shiftB = shiftsB.get(0);

        User temp = shiftA.getEmployee();
        shiftA.setEmployee(shiftB.getEmployee());
        shiftB.setEmployee(temp);

        scheduledShiftRepository.save(shiftA);
        scheduledShiftRepository.save(shiftB);
    }

    // Helper: remove scheduled shift
    private void removeScheduledShift(Long userId, LocalDate date) {
        List<ScheduledShift> shifts = scheduledShiftRepository.findByEmployeeIdAndDate(userId, date);
        scheduledShiftRepository.deleteAll(shifts);
    }

    // Convert entity → DTO
    private ShiftChangeResponseDTO toDTO(Request request) {

        String computedDayOfWeek = null;
        if (request.getDate() != null) {
            computedDayOfWeek = request.getDate().getDayOfWeek().name();
        }

        return new ShiftChangeResponseDTO(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getFullName(),
                request.getType(),
                request.getStatus(),
                request.getDate() != null ? request.getDate().toString() : null,
                request.getStartTime() != null ? request.getStartTime().toString() : null,
                request.getEndTime() != null ? request.getEndTime().toString() : null,
                request.getStartDate() != null ? request.getStartDate().toString() : null,
                request.getEndDate() != null ? request.getEndDate().toString() : null,
                request.getSwapWithUser() != null ? request.getSwapWithUser().getId() : null,
                request.getReason(),
                computedDayOfWeek
        );
    }
}