package com.example.shifter.model;

import com.example.shifter.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a one-time unavailability exception.
 * Used for sick days, leave requests, and one-time unavailable requests.
 * Automatically stores the day of the week based on the date.
 */
@Entity
@Table(name = "availability_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Employee who is unavailable */
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    /** Specific date of unavailability */
    @Column(nullable = false)
    private LocalDate date;

    /** Automatically derived day of week */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** Optional start time (null = all day) */
    private LocalTime startTime;

    /** Optional end time (null = all day) */
    private LocalTime endTime;

    /** Reason for unavailability */
    @Column(length = 255)
    private String reason;

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

    //Constructor
    public AvailabilityException(
            User employee,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) {
        this.employee = employee;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }


}
