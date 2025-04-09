package com.smart.attendance.controller;

import com.smart.attendance.model.Attendance;
import com.smart.attendance.model.User;
import com.smart.attendance.repository.AttendanceRepository;
import com.smart.attendance.repository.UserRepository;
import com.smart.attendance.service.LogService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LogService logService; // ✅ Inject LogService to save logs

    private final String IMAGE_DIRECTORY = "D:/attendance/faces/";
    private final String FASTAPI_BASE_URL = "http://localhost:8000";
    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

    public UserController() {
        File directory = new File(IMAGE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    // ✅ Register a new user and train their face
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("employeeId") String employeeId,
            @RequestParam("file") MultipartFile file) {
        try {
            LOGGER.info("📌 Registering new user: " + name + " (ID: " + employeeId + ")");
            
            // Check if email or employeeId already exists
            if (userRepository.findByEmail(email).isPresent()) {
                LOGGER.warning("❌ Email already in use: " + email);
                return ResponseEntity.badRequest().body("Email already in use!");
            }
            if (userRepository.findByEmployeeId(employeeId).isPresent()) {
                LOGGER.warning("❌ Employee ID already exists: " + employeeId);
                return ResponseEntity.badRequest().body("Employee ID already exists!");
            }

            // Ensure directory exists
            Files.createDirectories(Paths.get(IMAGE_DIRECTORY));

            // Save image permanently
            String fileName = employeeId + "_" + file.getOriginalFilename();
            File imageFile = new File(IMAGE_DIRECTORY + fileName);
            file.transferTo(imageFile);

            // Validate if the file is actually saved
            if (!imageFile.exists() || imageFile.length() == 0) {
                LOGGER.severe("❌ Error: Image file not saved correctly!");
                return ResponseEntity.internalServerError().body("Error: Image file not saved correctly!");
            }

            // Save user in database
            User user = User.builder()
                    .name(name.trim())
                    .email(email.trim())
                    .employeeId(employeeId.trim())
                    .imagePath(imageFile.getAbsolutePath())
                    .build();
            userRepository.save(user);
            LOGGER.info("✅ User saved in database: " + name + " (ID: " + employeeId + ")");
            
            // 🚀 Call FastAPI to train face
            ResponseEntity<String> fastApiResponse = trainFace(imageFile, employeeId, name);
            if (!fastApiResponse.getStatusCode().is2xxSuccessful()) {
                LOGGER.warning("⚠️ User registered, but FastAPI face training failed!");
                return ResponseEntity.internalServerError()
                        .body("User registered, but FastAPI face training failed!");
            }

            // ✅ Log this action
            logService.logActivity("User Registered", "User " + name + " (ID: " + employeeId + ") registered successfully.");
            return ResponseEntity.ok("User registered & trained successfully!");
        } catch (IOException e) {
            LOGGER.severe("❌ Error saving image: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error saving image: " + e.getMessage());
        }
    }

    // ✅ Train face using FastAPI
    private ResponseEntity<String> trainFace(File imageFile, String employeeId, String name) {
        LOGGER.info("🔍 Training face for user: " + name + " (ID: " + employeeId + ")");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile)); // ✅ Use FileSystemResource
        body.add("employee_id", employeeId);
        body.add("name", name);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String apiUrl = FASTAPI_BASE_URL + "/train_faces/";

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

        LOGGER.info("FastAPI Response: " + response.getStatusCode() + " - " + response.getBody());
        return response;
    }

    // ✅ Fetch all users
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        LOGGER.info("📌 Fetching all registered users.");
        List<User> users = userRepository.findAllByOrderByEmployeeIdAsc();
        return ResponseEntity.ok(users);
    }

    // ✅ Update user details
    @PatchMapping("/update/{employeeId}")
    public ResponseEntity<?> updateUser(
            @PathVariable String employeeId,
            @RequestBody Map<String, String> updates) {

        Optional<User> userOpt = userRepository.findByEmployeeId(employeeId);
        if (userOpt.isEmpty()) {
            LOGGER.warning("❌ User not found: " + employeeId);
            return ResponseEntity.badRequest().body("User not found!");
        }

        User user = userOpt.get();

        if (updates.containsKey("name")) {
            user.setName(updates.get("name").trim());
        }

        if (updates.containsKey("email")) {
            String newEmail = updates.get("email").trim();
            if (userRepository.findByEmail(newEmail).isPresent()) {
                LOGGER.warning("❌ Email already in use: " + newEmail);
                return ResponseEntity.badRequest().body("Email already in use!");
            }
            user.setEmail(newEmail);
        }

        userRepository.save(user);
        LOGGER.info("✅ User updated successfully: " + employeeId);
        
        logService.logActivity("User Updated", "User " + employeeId + " details updated.");
        return ResponseEntity.ok("User updated successfully!");
    }

    // ✅ Delete user and image
    @DeleteMapping("/delete/{employeeId}")
    public ResponseEntity<?> deleteUser(@PathVariable String employeeId) {
        Optional<User> userOpt = userRepository.findByEmployeeId(employeeId);
        if (userOpt.isEmpty()) {
            LOGGER.warning("❌ User not found: " + employeeId);
            return ResponseEntity.badRequest().body("User not found!");
        }

        User user = userOpt.get();
        File imageFile = new File(user.getImagePath());

        if (imageFile.exists() && !imageFile.delete()) {
            LOGGER.severe("❌ Error deleting image for user: " + employeeId);
            return ResponseEntity.internalServerError().body("Error: Unable to delete user image!");
        }

        userRepository.delete(user);
        LOGGER.info("✅ User deleted: " + employeeId);

        logService.logActivity("User Deleted", "User " + employeeId + " was deleted.");
        return ResponseEntity.ok("User and image deleted successfully!");
    }

    @GetMapping("/stats/{employeeId}")
public ResponseEntity<?> getUserStats(@PathVariable String employeeId) {
    User user = userRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    List<Attendance> attendanceRecords = attendanceRepository.findByUserEmployeeId(employeeId);

    long totalDays = attendanceRecords.size();
    long presentDays = attendanceRecords.stream()
            .filter(attendance -> attendance.getStatus().equalsIgnoreCase("On Time") ||
                                  attendance.getStatus().equalsIgnoreCase("Late"))
            .count();

    double attendancePercentage = (totalDays > 0) ? ((double) presentDays / totalDays) * 100 : 0;

    Map<String, Object> stats = new HashMap<>();
    stats.put("employeeId", user.getEmployeeId());
    stats.put("name", user.getName());
    stats.put("email", user.getEmail());
    stats.put("totalDays", totalDays);
    stats.put("presentDays", presentDays);
    stats.put("attendancePercentage", String.format("%.2f", attendancePercentage));
    stats.put("attendanceRecords", attendanceRecords);

    return ResponseEntity.ok(stats);
}


@GetMapping("/stats/export-csv/{employeeId}")
public void exportUserStatsAsCSV(@PathVariable String employeeId, HttpServletResponse response) {
    try {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Attendance> attendanceRecords = attendanceRepository.findByUserEmployeeId(employeeId);

        // Set response headers
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + employeeId + "_stats.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Employee ID, Name, Email, Total Days, Present Days, Attendance Percentage");
        writer.printf("%s,%s,%s,%d,%d,%.2f%%\n",
                user.getEmployeeId(), user.getName(), user.getEmail(),
                attendanceRecords.size(),
                attendanceRecords.stream().filter(a -> a.getStatus().equalsIgnoreCase("On Time") || a.getStatus().equalsIgnoreCase("Late")).count(),
                ((double) attendanceRecords.stream().filter(a -> a.getStatus().equalsIgnoreCase("On Time") || a.getStatus().equalsIgnoreCase("Late")).count() / attendanceRecords.size()) * 100
        );

        // Add attendance records
        writer.println("\nDate, Status");
        for (Attendance record : attendanceRecords) {
            writer.printf("%s,%s\n", record.getScannedAt(), record.getStatus());
        }

        writer.flush();
    } catch (Exception e) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
}
