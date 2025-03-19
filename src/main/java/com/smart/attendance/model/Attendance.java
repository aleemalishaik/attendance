package com.smart.attendance.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "employeeId", nullable = false)
    private User user;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;  // Use LocalDateTime to store both date and time

    @Column(nullable = false)
    private String status; // "On Time", "Late", "Absent"
}
