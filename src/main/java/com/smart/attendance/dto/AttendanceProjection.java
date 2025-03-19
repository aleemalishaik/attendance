package com.smart.attendance.dto;

import java.time.LocalDateTime;

public interface AttendanceProjection {
    String getEmployeeId();  // ✅ Fetch from `User`
    String getName();        // ✅ Fetch from `User`
    LocalDateTime getScannedAt();  // ✅ Fix: Use `LocalDateTime`
    String getStatus();
}
