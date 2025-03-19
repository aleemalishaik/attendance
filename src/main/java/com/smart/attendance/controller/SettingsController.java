package com.smart.attendance.controller;

import com.smart.attendance.model.Settings;
import com.smart.attendance.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());

    @Autowired
    private SettingsRepository settingsRepository;

    // Fetch Current System Settings
    @GetMapping
    public ResponseEntity<Settings> getSettings() {
        LOGGER.info("🔍 Fetching current system settings");

        Optional<Settings> settingsOpt = settingsRepository.findById(1);  // Only one settings record expected

        if (settingsOpt.isPresent()) {
            LOGGER.info("✅ Settings found: " + settingsOpt.get());
            return ResponseEntity.ok(settingsOpt.get());
        } else {
            LOGGER.warning("❌ Settings not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Settings());  // Always return an empty settings if not found
        }
    }

    // ✅ Create New Settings (Only If Not Exists)
    @PostMapping
    @Transactional
    public ResponseEntity<?> createSettings(@RequestBody Settings newSettings) {
        LOGGER.info("📋 Creating new system settings");

        // Check if settings already exist
        if (settingsRepository.count() > 0) {
            LOGGER.warning("❌ Settings already exist");
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Settings already exist! Use update instead.");
        }

        // Validate input times
        if (newSettings.getEndTime().isBefore(newSettings.getStartTime())) {
            LOGGER.warning("❌ End time cannot be before start time");
            return ResponseEntity.badRequest().body("End time cannot be before start time!");
        }
        if (newSettings.getLateLimit().isBefore(newSettings.getOnTimeLimit())) {
            LOGGER.warning("❌ Late limit must be after on-time limit");
            return ResponseEntity.badRequest().body("Late limit must be after on-time limit!");
        }

        // Save new settings
        settingsRepository.save(newSettings);
        LOGGER.info("✅ New settings created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body("Settings created successfully!");
    }

    // ✅ Update Existing Settings (Ensuring Only One Record)
    @PutMapping
    @Transactional
    public ResponseEntity<?> updateSettings(@RequestBody Settings newSettings) {
        LOGGER.info("📝 Updating system settings");

        // Validate input times
        if (newSettings.getEndTime().isBefore(newSettings.getStartTime())) {
            LOGGER.warning("❌ End time cannot be before start time");
            return ResponseEntity.badRequest().body("End time cannot be before start time!");
        }
        if (newSettings.getLateLimit().isBefore(newSettings.getOnTimeLimit())) {
            LOGGER.warning("❌ Late limit must be after on-time limit");
            return ResponseEntity.badRequest().body("Late limit must be after on-time limit!");
        }

        // Check if settings exist and update the existing record
        Optional<Settings> existingSettingsOpt = settingsRepository.findById(1);  // We expect only one settings entry
        if (existingSettingsOpt.isPresent()) {
            Settings existingSettings = existingSettingsOpt.get();
            // Update the settings fields
            existingSettings.setStartTime(newSettings.getStartTime());
            existingSettings.setEndTime(newSettings.getEndTime());
            existingSettings.setOnTimeLimit(newSettings.getOnTimeLimit());
            existingSettings.setLateLimit(newSettings.getLateLimit());

            // Save the updated settings
            settingsRepository.save(existingSettings);
            LOGGER.info("✅ Settings updated successfully");
            return ResponseEntity.ok("Settings updated successfully!");
        } else {
            LOGGER.warning("❌ Settings not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Settings not found!");
        }
    }

    // Delete System Settings
    @DeleteMapping
    public ResponseEntity<?> deleteSettings() {
        LOGGER.info("🗑️ Deleting system settings");

        settingsRepository.deleteAll();  // Clear all settings
        LOGGER.info("✅ System settings deleted successfully");
        return ResponseEntity.ok("System settings deleted!");
    }
}
