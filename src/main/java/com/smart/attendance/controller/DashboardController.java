package com.smart.attendance.controller;

import com.smart.attendance.model.User;
import com.smart.attendance.repository.AttendanceRepository;
import com.smart.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")  
public class DashboardController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ 1. API to fetch today's attendance summary (For Pie Chart)
    @GetMapping("/today-summary")
    public Map<String, Integer> getTodayAttendanceSummary() {
        LocalDate today = LocalDate.now();

        // Use "On-Time", "Late", "Absent" statuses instead of "Present"
        int onTime = attendanceRepository.countByStatusAndDate("On Time", today);
        int absent = attendanceRepository.countByStatusAndDate("Absent", today);
        int late = attendanceRepository.countByStatusAndDate("Late", today);

        Map<String, Integer> summary = new HashMap<>();
        summary.put("onTime", onTime);
        summary.put("absent", absent);
        summary.put("late", late);

        return summary;
    }

    // ✅ 2. API to fetch hourly attendance (For Hourly Chart)
    @GetMapping("/hourly-attendance")
    public Map<String, Object> getHourlyAttendance() {
        LocalDate today = LocalDate.now();

        Map<String, Object> hourlyData = new HashMap<>();
        for (int i = 0; i < 24; i++) { // Fetch data for the first 12 hours
            LocalTime startHour = LocalTime.of(i, 0); // Starting hour
            LocalTime endHour = startHour.plusHours(1); // Ending hour

            // Convert LocalDate and LocalTime to LocalDateTime (including time)
            LocalDateTime startDateTime = LocalDateTime.of(today, startHour);
            LocalDateTime endDateTime = LocalDateTime.of(today, endHour);

            // Fetch counts from the repository for On-Time, Late, and Absent
            int onTimeCount = attendanceRepository.countByStatusAndTimeRange("On Time", startDateTime, endDateTime);
            int absentCount = attendanceRepository.countByStatusAndTimeRange("Absent", startDateTime, endDateTime);
            int lateCount = attendanceRepository.countByStatusAndTimeRange("Late", startDateTime, endDateTime);

            // Add the data for each hour
            hourlyData.put("hour" + i, Map.of(
                    "onTime", onTimeCount,
                    "absent", absentCount,
                    "late", lateCount));
        }

        return hourlyData;
    }

    // ✅ 3. API to fetch weekly attendance trend (For Line Chart)
    @GetMapping("/weekly-trend")
    public Map<String, Object> getWeeklyAttendanceTrend() {
        LocalDate today = LocalDate.now();
        List<Integer> onTimeData = new ArrayList<>();
        List<Integer> absentData = new ArrayList<>();
        List<Integer> lateData = new ArrayList<>();
        List<String> days = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            days.add(day.getDayOfWeek().toString().substring(0, 3)); // Mon, Tue, ...

            // Fetch data for "On-Time", "Absent", and "Late"
            int onTime = attendanceRepository.countByStatusAndDate("On Time", day);
            int absent = attendanceRepository.countByStatusAndDate("Absent", day);
            int late = attendanceRepository.countByStatusAndDate("Late", day);

            onTimeData.add(onTime);
            absentData.add(absent);
            lateData.add(late);
        }

        Map<String, Object> weeklyData = new HashMap<>();
        weeklyData.put("days", days); // List of days (Mon, Tue, ...)
        weeklyData.put("onTime", onTimeData); // List of On-Time attendance counts
        weeklyData.put("absent", absentData); // List of Absent attendance counts
        weeklyData.put("late", lateData); // List of Late attendance counts

        return weeklyData;
    }

    // ✅ 4. API to fetch leaderboard (Top 5 employees based on attendance performance)
    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        // Fetch all users
        List<User> users = userRepository.findAll();

        // List to hold leaderboard entries
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        // Loop over all users to calculate their attendance performance
        for (User user : users) {
            // Calculate total days for each user
            int totalDays = attendanceRepository.countByEmployeeId(user.getEmployeeId()); 

            // Calculate days marked as "On-Time", "Late", and "Absent"
            int onTimeDays = attendanceRepository.countByStatusAndEmployeeId("On Time", user.getEmployeeId());
            int lateDays = attendanceRepository.countByStatusAndEmployeeId("Late", user.getEmployeeId());
            int absentDays = attendanceRepository.countByStatusAndEmployeeId("Absent", user.getEmployeeId());

            // Calculate performance score (percentage of On-Time attendance)
            double performanceScore = (totalDays > 0) ? ((double) onTimeDays / totalDays) * 100 : 0;

            // Prepare leaderboard entry
            Map<String, Object> entry = new HashMap<>();
            entry.put("employeeId", user.getEmployeeId());
            entry.put("name", user.getName());
            entry.put("onTimeDays", onTimeDays);
            entry.put("lateDays", lateDays);
            entry.put("absentDays", absentDays);
            entry.put("performanceScore", String.format("%.2f%%", performanceScore)); // Attendance performance score

            // Add the entry to the leaderboard list
            leaderboard.add(entry);
        }

        // Sort leaderboard by performance score (on-time attendance percentage) in descending order
        return leaderboard.stream()
                .sorted((a, b) -> Double.compare(
                        Double.parseDouble(b.get("performanceScore").toString().replace("%", "")),
                        Double.parseDouble(a.get("performanceScore").toString().replace("%", ""))))
                .limit(5) // Limit to the top 5 employees based on performance
                .collect(Collectors.toList());
    }
}
