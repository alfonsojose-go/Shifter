package com.example.shifter.repository.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.shifter.model.availability.Availability;

import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    
    List<Availability> findByEmployeeId(Long employeeId);
    
    List<Availability> findByEmployeeIdAndDayOfWeek(Long employeeId, Availability.DayOfWeek dayOfWeek);

    @Query("""
        SELECT a FROM Availability a
        WHERE a.employee.id = :employeeId
          AND a.dayOfWeek = :dayOfWeek
          AND a.startTime < :endTime
          AND :startTime < a.endTime
          AND (:excludeId IS NULL OR a.availabilityId <> :excludeId)
    """)
    List<Availability> findOverlappingAvailabilities(
            @Param("employeeId") Long employeeId,
            @Param("dayOfWeek") Availability.DayOfWeek dayOfWeek,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeId") Long excludeId
    );
    
    boolean existsByEmployeeId(Long employeeId);
    
    long countByEmployeeId(Long employeeId);
    
    @Query("SELECT DISTINCT a.employee FROM Availability a WHERE a.dayOfWeek = :dayOfWeek")
    List<com.example.shifter.model.User> findAvailableEmployeesByDay(@Param("dayOfWeek") Availability.DayOfWeek dayOfWeek);

    List<Availability> findByDayOfWeek(Availability.DayOfWeek dayOfWeek);

    int deleteByEmployeeId(Long employeeId);

}