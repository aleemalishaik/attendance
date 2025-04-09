package com.smart.attendance.repository;

import com.smart.attendance.dto.AttendanceProjection;
import com.smart.attendance.model.Attendance;
import com.smart.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        // ✅ Fix: Use scannedAt instead of date
        Optional<Attendance> findFirstByUserAndScannedAtBetween(User user, LocalDateTime start, LocalDateTime end);

        // ✅ Fix: Projection should match `scannedAt`
        @Query("""
                            SELECT a.user.employeeId AS employeeId,
                                   a.user.name AS name,
                                   a.scannedAt AS scannedAt,
                                   a.status AS status
                            FROM Attendance a
                            ORDER BY a.scannedAt DESC
                        """)
        List<AttendanceProjection> findAllByOrderByScannedAtDesc();

        @Query("SELECT COUNT(a) FROM Attendance a WHERE FUNCTION('DATE', a.scannedAt) = :date")
        long countByDate(@Param("date") LocalDate date);

        long countByScannedAtAndStatus(LocalDate today, String status); // ✅ Count successful attendance for today

        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.scannedAt >= :startOfDay AND a.scannedAt < :endOfDay AND a.status = :status")
        long countByDateAndStatus(@Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay,
                        @Param("status") String status);

        // ✅ Count attendance by status and date (For Pie Chart & Weekly Trend)
        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = :status AND DATE(a.scannedAt) = :date")
        int countByStatusAndDate(@Param("status") String status, @Param("date") LocalDate date);

        // ✅ Corrected query to access employeeId via the User entity
        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = :status AND a.user.employeeId = :employeeId")
        int countByStatusAndEmployeeId(@Param("status") String status, @Param("employeeId") String employeeId);

        // ✅ Corrected query to access employeeId via the User entity (alternative)
        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user.employeeId = :employeeId")
        int countByEmployeeId(@Param("employeeId") String employeeId);

        @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = :status " +
                        "AND a.scannedAt >= :startDateTime AND a.scannedAt <= :endDateTime")
        int countByStatusAndTimeRange(@Param("status") String status,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime);

        List<Attendance> findByUserEmployeeId(String employeeId); // Corrected method to use `user.employeeId`

}
