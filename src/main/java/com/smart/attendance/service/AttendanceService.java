package com.smart.attendance.service;

import com.smart.attendance.model.Attendance;
import com.smart.attendance.model.Settings;
import com.smart.attendance.model.User;
import com.smart.attendance.repository.AttendanceRepository;
import com.smart.attendance.repository.SettingsRepository;
import com.smart.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    // ✅ **Mark Attendance After Face Recognition**
    public ResponseEntity<?> markAttendance(String employeeId) {
        Optional<User> userOpt = userRepository.findByEmployeeId(employeeId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in database!");
        }
        User user = userOpt.get();

        // ✅ Get Current Timestamp
        LocalDateTime now = LocalDateTime.now();

        // ✅ Check if attendance for today is already marked
        Optional<Attendance> existingAttendance = attendanceRepository.findFirstByUserAndScannedAtBetween(
                user,
                now.toLocalDate().atStartOfDay(),
                now.toLocalDate().atTime(23, 59, 59)
        );

        if (existingAttendance.isPresent()) {
            return ResponseEntity.ok(Map.of("message", "Attendance already marked for Employee ID: " + employeeId));
        }

        // ✅ Determine attendance status
        String status = determineAttendanceStatus();

        // ✅ Mark attendance with `scannedAt`
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setScannedAt(now); // Store both date & time
        attendance.setStatus(status);
        attendanceRepository.save(attendance);

        return ResponseEntity
                .ok(Map.of("message", "Attendance marked as " + status + " for Employee ID: " + employeeId));
    }

    // ✅ **Determine "On Time" / "Late" based on settings**
    public String determineAttendanceStatus() {
        Optional<Settings> settingsOpt = settingsRepository.findById(1);
        if (settingsOpt.isEmpty()) {
            return "Unknown";
        }

        Settings settings = settingsOpt.get();
        LocalTime currentTime = LocalTime.now();

        if (currentTime.isBefore(settings.getOnTimeLimit()) || currentTime.equals(settings.getOnTimeLimit())) {
            return "On Time";
        } else if (currentTime.isBefore(settings.getLateLimit())) {
            return "Late";
        } else {
            return "Absent";
        }
    }

    
}
