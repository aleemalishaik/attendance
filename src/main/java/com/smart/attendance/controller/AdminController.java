package com.smart.attendance.controller;

import com.smart.attendance.model.Admin;
import com.smart.attendance.repository.AdminRepository;
import com.smart.attendance.security.JwtUtil;
import com.smart.attendance.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogService logService;

    private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());

    // Register Admin (Includes Email)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Admin admin) {
        LOGGER.info("📌 Registering new admin: " + admin.getUsername());

        if (adminRepository.findByUsername(admin.getUsername()).isPresent()) {
            LOGGER.warning("❌ Username already exists: " + admin.getUsername());
            return ResponseEntity.badRequest().body("❌ Username already exists!");
        }
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            LOGGER.warning("❌ Email already in use: " + admin.getEmail());
            return ResponseEntity.badRequest().body("❌ Email already in use!");
        }

        admin.setPassword(passwordEncoder.encode(admin.getPassword())); // Secure password
        admin.setCreatedAt(LocalDateTime.now());

        adminRepository.save(admin);
        LOGGER.info("✅ Admin registered successfully: " + admin.getUsername());

        logService.logActivity("Admin Has Registered Successfully", "" + admin.getUsername());
        return ResponseEntity.ok("Admin registered successfully!");
    }

    // Admin Login (Supports Username or Email)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier"); // Username or Email
        String password = request.get("password");

        LOGGER.info("🔍 Attempting login for: " + identifier);

        Optional<Admin> adminOpt = adminRepository.findByUsername(identifier);
        if (adminOpt.isEmpty()) {
            adminOpt = adminRepository.findByEmail(identifier);
            if (adminOpt.isEmpty()) {
                LOGGER.warning("❌ Admin not found: " + identifier);
                return ResponseEntity.badRequest().body("Admin not found!");
            }
        }

        Admin admin = adminOpt.get();
        LOGGER.info("Found admin. Checking password...");

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            LOGGER.warning("❌ Invalid password for admin: " + identifier);
            return ResponseEntity.badRequest().body("Invalid credentials!");
        }

        String token = jwtUtil.generateToken(admin.getUsername());
        LOGGER.info("✅ Admin logged in successfully: " + admin.getUsername());

        logService.logActivity("Admin Has Logged In", "Admin name is " + admin.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }

    // Get All Admins (Sorted by ID)
    @GetMapping("/all")
    public ResponseEntity<List<Admin>> getAllAdmins() {
        LOGGER.info("📌 Fetching all admins.");
        List<Admin> admins = adminRepository.findAllByOrderByIdAsc();
        return ResponseEntity.ok(admins);
    }

    // Update Admin Details (Username, Email, Password)
    @PatchMapping("/update/{id}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Optional<Admin> adminOpt = adminRepository.findById(id);
        if (adminOpt.isEmpty()) {
            LOGGER.warning("❌ Admin not found with ID: " + id);
            return ResponseEntity.badRequest().body("❌ Admin not found!");
        }

        Admin admin = adminOpt.get();

        // Update username (only if it's provided and not empty)
        if (updates.containsKey("username") && !updates.get("username").trim().isEmpty()) {
            String newUsername = updates.get("username").trim();
            if (!newUsername.equals(admin.getUsername()) && adminRepository.findByUsername(newUsername).isPresent()) {
                LOGGER.warning("❌ Username already exists: " + newUsername);
                return ResponseEntity.badRequest().body("❌ Username already exists!");
            }
            admin.setUsername(newUsername);
        }

        // Update email (only if it's provided and not empty)
        if (updates.containsKey("email") && !updates.get("email").trim().isEmpty()) {
            String newEmail = updates.get("email").trim();
            if (!newEmail.equals(admin.getEmail()) && adminRepository.findByEmail(newEmail).isPresent()) {
                LOGGER.warning("❌ Email already in use: " + newEmail);
                return ResponseEntity.badRequest().body("❌ Email already in use!");
            }
            admin.setEmail(newEmail);
        }

        // Update password (only if it's provided and not empty)
        if (updates.containsKey("password") && !updates.get("password").trim().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(updates.get("password").trim()));
        }

        // Save updated admin
        adminRepository.save(admin);
        LOGGER.info("✅ Admin updated successfully: " + admin.getUsername());

        logService.logActivity("Admin Has Updated Successfully", "Admin name is " + admin.getUsername());

        return ResponseEntity.ok("Admin details updated successfully!");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAdmin(@RequestParam(required = false) String username,
                                         @RequestParam(required = false) String email) {
        if (username == null && email == null) {
            LOGGER.warning("❌ Please provide a username or email to delete.");
            return ResponseEntity.badRequest().body("❌ Please provide a username or email to delete.");
        }

        Optional<Admin> adminOpt = Optional.empty();

        if (username != null) {
            adminOpt = adminRepository.findByUsername(username);
        } else if (email != null) {
            adminOpt = adminRepository.findByEmail(email);
        }

        if (adminOpt.isEmpty()) {
            LOGGER.warning("❌ Admin not found: " + (username != null ? username : email));
            return ResponseEntity.badRequest().body("❌ Admin not found!");
        }

        logService.logActivity("Admin Has been deleted successfully from the database", adminOpt.get().getUsername());
        adminRepository.delete(adminOpt.get());
        LOGGER.info("✅ Admin deleted successfully: " + (username != null ? username : email));
        return ResponseEntity.ok("Admin deleted successfully!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
        }

        String username = jwtUtil.extractUsername(token);
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);

        if (adminOpt.isEmpty()) {
            LOGGER.warning("❌ Admin not found for token: " + username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Admin not found!");
        }

        Admin admin = adminOpt.get();
        LOGGER.info("✅ Fetching current admin: " + admin.getUsername());
        return ResponseEntity.ok(admin);
    }

    @PatchMapping("/update-password/{id}")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Optional<Admin> adminOpt = adminRepository.findById(id);
        if (adminOpt.isEmpty()) {
            LOGGER.warning("❌ Admin not found with ID: " + id);
            return ResponseEntity.badRequest().body("❌ Admin not found!");
        }

        Admin admin = adminOpt.get();
        String newPassword = updates.get("password");

        if (newPassword == null || newPassword.trim().isEmpty()) {
            LOGGER.warning("❌ Password cannot be empty for admin: " + admin.getUsername());
            return ResponseEntity.badRequest().body("❌ Password cannot be empty!");
        }

        // Hash the new password before storing
        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);

        LOGGER.info("✅ Password updated successfully for admin: " + admin.getUsername());
        return ResponseEntity.ok("Password updated successfully!");
    }
}
