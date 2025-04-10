package com.smart.attendance.controller;

import com.smart.attendance.dto.AttendanceProjection;
import com.smart.attendance.model.Attendance;
import com.smart.attendance.model.User;
import com.smart.attendance.repository.AttendanceRepository;
import com.smart.attendance.repository.UserRepository;
import com.smart.attendance.service.AttendanceService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    // Logger for the class
    private static final Logger LOGGER = Logger.getLogger(AttendanceController.class.getName());

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceService attendanceService;

    // ✅ **Mark Attendance After Face Recognition**
    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, String> request) {
        String employeeId = request.get("employeeId");
        LOGGER.info("📌 Marking attendance for Employee ID: " + employeeId);

        // ✅ Find user by employeeId (Unique)
        Optional<User> userOpt = userRepository.findByEmployeeId(employeeId);
        if (userOpt.isEmpty()) {
            LOGGER.warning("❌ User not found in database for Employee ID: " + employeeId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in database!");
        }
        User user = userOpt.get();

        // ✅ Get Current Timestamp
        LocalDateTime now = LocalDateTime.now();
        LOGGER.info("🕒 Current time for attendance marking: " + now);

        // ✅ Check if attendance for today is already marked
        Optional<Attendance> existingAttendance = attendanceRepository.findFirstByUserAndScannedAtBetween(
                user,
                now.toLocalDate().atStartOfDay(),
                now.toLocalDate().atTime(23, 59, 59));

        if (existingAttendance.isPresent()) {
            LOGGER.info("❌ Attendance already marked for Employee ID: " + employeeId);
            return ResponseEntity.ok(Map.of("message", "Attendance already marked for Employee ID: " + employeeId));
        }

        // ✅ Determine attendance status
        String status = attendanceService.determineAttendanceStatus();
        LOGGER.info("✅ Attendance status for Employee ID " + employeeId + ": " + status);

        // ✅ Mark attendance with `scannedAt`
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setScannedAt(now); // Now stores date + time
        attendance.setStatus(status);
        attendanceRepository.save(attendance);
        LOGGER.info("✅ Attendance marked successfully for Employee ID: " + employeeId + " with status: " + status);

        return ResponseEntity
                .ok(Map.of("message", "Attendance marked as " + status + " for Employee ID: " + employeeId));
    }

    // ✅ Get all attendance records (Ordered by scannedAt Descending)
    @GetMapping("/all")
    public ResponseEntity<List<AttendanceProjection>> getAllAttendanceRecords() {
        LOGGER.info("📌 Fetching all attendance records ordered by scanned time.");
        List<AttendanceProjection> attendanceRecords = attendanceRepository.findAllByOrderByScannedAtDesc();
        return ResponseEntity.ok(attendanceRecords);
    }

    // ✅ **Get Attendance Stats for Today**
    @GetMapping("/stats")
    public Map<String, Long> getAttendanceStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        LOGGER.info("📊 Fetching attendance stats for today: " + today);

        // Fetching attendance stats for On-Time, Late, and Absent
        long totalAttendance = attendanceRepository.count(); // Overall attendance
        long todayTotal = attendanceRepository.countByDate(today); // Today's total
        long onTimeToday = attendanceRepository.countByDateAndStatus(startOfDay, endOfDay, "On Time"); // On-Time today
        long lateToday = attendanceRepository.countByDateAndStatus(startOfDay, endOfDay, "Late"); // Late today
        long absentToday = attendanceRepository.countByDateAndStatus(startOfDay, endOfDay, "Absent"); // Absent today

        // Prepare response
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalAttendance", totalAttendance);
        stats.put("todayTotal", todayTotal);
        stats.put("onTimeToday", onTimeToday);
        stats.put("lateToday", lateToday);
        stats.put("absentToday", absentToday);

        LOGGER.info("✅ Attendance stats for today: Total - " + todayTotal + ", On Time - " + onTimeToday + ", Late - " + lateToday + ", Absent - " + absentToday);

        return stats;
    }
}
