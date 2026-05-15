package com.example.shifter.repository;

import com.example.shifter.model.ClockRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClockRecordRepository extends JpaRepository<ClockRecord, Long> {

    /** Find today's active clock-in for a user */
    Optional<ClockRecord> findByUserIdAndWorkDateAndActive(Long userId, LocalDate date, boolean active);

    /** Find the most recent record for a user */
    Optional<ClockRecord> findTopByUserIdOrderByIdDesc(Long userId);

    List<ClockRecord> findByWorkDateBetween(LocalDate start, LocalDate end);

    List<ClockRecord> findByUserIdAndWorkDateBetween(Long userId, LocalDate start, LocalDate end);
}