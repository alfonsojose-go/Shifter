package com.example.shifter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a scheduled shift on a specific date.
 * This is different from the weekly recurring schedule.
 * Automatically stores the day of the week based on the date.
 */
@Entity
@Table(name = "scheduled_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Employee assigned to this shift */
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    /** Specific date of the shift */
    @Column(nullable = false)
    private LocalDate date;

    /** Automatically derived day of week */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** Start time */
    @Column(nullable = false)
    private LocalTime startTime;

    /** End time */
    @Column(nullable = false)
    private LocalTime endTime;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Automatically set dayOfWeek before saving */
    @PrePersist
    @PreUpdate
    private void updateDayOfWeek() {
        if (this.date != null) {
            this.dayOfWeek = this.date.getDayOfWeek();
        }
    }

    // Constructor
    public ScheduledShift(User employee, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.employee = employee;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

}
