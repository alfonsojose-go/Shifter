package com.example.shifter.model;

import com.example.shifter.enums.RequestStatus;
import com.example.shifter.enums.RequestType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * This Entity stores the requests for shift change
 * time off, wants to work, swap, etc.
 */
@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Employee who submitted the request */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** For single-day requests */
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    /** For multi-day leave requests */
    private LocalDate startDate;
    private LocalDate endDate;

    /** For shift swap requests */
    @ManyToOne
    @JoinColumn(name = "swap_with_user_id")
    private User swapWithUser;

    /** Reason for leave or swap */
    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;


}
