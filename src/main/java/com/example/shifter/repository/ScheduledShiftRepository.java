package com.example.shifter.repository;

import com.example.shifter.model.ScheduledShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * This is for the shift change request
 *  * Repository for scheduled shifts.
 *  * Used for shift assignment, reporting, and labor cost calculations.
 */
public interface ScheduledShiftRepository extends JpaRepository<ScheduledShift, Long> {
    /** Used for shift-change requests */
    List<ScheduledShift> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    /** Fetch all shifts for a specific date */
    List<ScheduledShift> findByDate(LocalDate date);

    /** Fetch all shifts within a date range (required for weekly reports) */
    List<ScheduledShift> findByDateBetween(LocalDate start, LocalDate end);

    /**
     * For attendance report
     */
    List<ScheduledShift> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate start, LocalDate end);

    /**
     * For fetching data in ScheduledShift entity
     */
    /**
     * Fetches ALL shifts with employee + position in one query.
     *
     * JOIN FETCH s.employee      → avoids N+1 on User
     * LEFT JOIN FETCH e.position → LEFT because position is nullable;
     *                              inner join would silently drop employees
     *                              with no position assigned
     */
    @Query("SELECT s FROM ScheduledShift s JOIN FETCH s.employee e LEFT JOIN FETCH e.position")
    List<ScheduledShift> findAllWithEmployeeAndPosition();

    /**
     * Fetches shifts for ONE employee with their position in one query.
     */
    @Query("SELECT s FROM ScheduledShift s JOIN FETCH s.employee e LEFT JOIN FETCH e.position WHERE e.id = :employeeId")
    List<ScheduledShift> findByEmployeeIdWithPosition(@Param("employeeId") Long employeeId);


    /**
     * Posts in ScheduledShift entity.
     */
    List<ScheduledShift> findByEmployeeId(Long employeeId);
    
    
    boolean existsByEmployeeIdAndDateAndStartTime(
            Long employeeId, LocalDate date, LocalTime startTime);

    // Samara - Fix to the overlap of schedule of Alfonso's code
    // Returns all shifts that overlap the given time range for the same employee.
    // Overlap rule: start1 < end2 AND start2 < end1
    @Query("""
    SELECT s FROM ScheduledShift s
    WHERE s.employee.id = :employeeId
      AND s.date = :date
      AND s.startTime < :endTime
      AND s.endTime > :startTime
""")
    List<ScheduledShift> findOverlappingShifts(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

}
