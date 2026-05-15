package com.example.shifter.model.scheduling;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.shifter.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "Schedule")
@ToString(exclude = "employee")
@Getter
@Setter
@NoArgsConstructor
public class Scheduling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long schedulingId;

    // Many-to-One: Many availabilities belong to ONE employee
    @ManyToOne
    @NotNull(message = "Employee is required")
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    
    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    
    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

        // Enum for days of week
    public enum DayOfWeek {
        MONDAY, 
        TUESDAY, 
        WEDNESDAY, 
        THURSDAY,
        FRIDAY, 
        SATURDAY, 
        SUNDAY
    }

    //Constructor is in Lombok @NoArgsConstructor


    //Required arguments for constructor
    public Scheduling(User employee, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.employee = employee;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    //Getters and Setters are in Lombok @Getter and @Setter


}
