package com.example.shifter.repository.position_scheduling;

import com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO;
import com.example.shifter.model.scheduling.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionSchedulingRepository extends JpaRepository<Scheduling, Long> {

    /**
     * Fetches all schedules and eagerly loads each employee's Position.
     */
    @Query("SELECT s FROM Scheduling s JOIN FETCH s.employee e LEFT JOIN FETCH e.position")
    List<Scheduling> findAllWithEmployeeAndPosition();

    /**
     * Find schedules by employee ID and map to EmployeeScheduleDTO
     * Using LEFT JOIN to ensure employees without positions are included
     */
    @Query("SELECT new com.example.shifter.dto.position_scheduling.EmployeeScheduleDTO(" +
           "s.employee.id, " +
           "s.employee.fullName, " +
           "s.employee.position.name, " +        // This will be null for Bob - that's OK
           "s.employee.position.hourlyWage, " +  // This will be null for Bob - that's OK
           "s.schedulingId, " +
           "s.dayOfWeek, " +
           "s.startTime, " +
           "s.endTime) " +
           "FROM Scheduling s " +
           "LEFT JOIN s.employee e " +            // LEFT JOIN to include all schedules
           "LEFT JOIN e.position p " +             // LEFT JOIN to handle null positions
           "WHERE s.employee.id = :employeeId")
    List<EmployeeScheduleDTO> findByEmployeeIdWithPosition(@Param("employeeId") Long employeeId);
}