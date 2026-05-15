package com.example.shifter.repository.scheduling;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.shifter.model.scheduling.Scheduling;

@Repository
public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    List<Scheduling> findByEmployeeId(Long employeeId);

    List<Scheduling> findByEmployeeIdAndDayOfWeek(Long employeeId, Scheduling.DayOfWeek dayOfWeek);
    
    @Query("SELECT a FROM Scheduling a WHERE a.employee.id = :employeeId " +
           "AND a.dayOfWeek = :dayOfWeek " +
           "AND (:startTime < a.endTime AND :endTime > a.startTime) " +
           "AND (:excludeId IS NULL OR a.schedulingId != :excludeId)")
    List<Scheduling> findOverlappingSchedules(
            @Param("employeeId") Long employeeId,
            @Param("dayOfWeek") Scheduling.DayOfWeek dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeId") Long excludeId);
    
    boolean existsByEmployeeId(Long employeeId);
    
    long countByEmployeeId(Long employeeId);
    
    @Query("SELECT DISTINCT a.employee FROM Scheduling a WHERE a.dayOfWeek = :dayOfWeek")
    List<com.example.shifter.model.User> findAvailableEmployeesByDay(@Param("dayOfWeek") Scheduling.DayOfWeek dayOfWeek);

    List<Scheduling> findByDayOfWeek(Scheduling.DayOfWeek dayOfWeek);

    int deleteByEmployeeId(Long employeeId);


}
