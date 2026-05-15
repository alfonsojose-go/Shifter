package com.example.shifter.model;

import com.example.shifter.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clock_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClockRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Employee who clocked in/out */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Date of the workday (e.g., 2026-02-22)
     * allows grouping by day
     * */
    @Column(nullable = false)
    private LocalDate workDate;

    /** Timestamp when user clocked in */
    @Column(nullable = false)
    private LocalDateTime clockInTime;

    /** Timestamp when user clocked out */
    private LocalDateTime clockOutTime;

    /** Whether the user is currently clocked in
     * - active = true means the user is clocked in.
     * - active = false means the user clocked out.
     * */
    @Column(nullable = false)
    private boolean active;
}