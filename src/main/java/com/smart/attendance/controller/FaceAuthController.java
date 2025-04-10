package com.smart.attendance.controller;

import com.smart.attendance.config.AppProperties;
import com.smart.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/face_auth")
public class FaceAuthController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AppProperties appProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger LOGGER = Logger.getLogger(FaceAuthController.class.getName());

    // ✅ **Scan Face & Mark Attendance Automatically**
    @PostMapping("/scan")
    public ResponseEntity<?> scanFace(@RequestParam("file") MultipartFile file) {
        try {
            // ✅ Save file temporarily to ensure correct format
            File tempFile = File.createTempFile("scan_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            LOGGER.info("📡 Sending image to FastAPI: " + tempFile.getAbsolutePath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile)); // ✅ Use FileSystemResource

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String apiUrl = appProperties.getFastapiUrl() + "/recognize_face/";
            // ✅ Call FastAPI
            LOGGER.info("🔍 Calling FastAPI for face recognition...");
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, requestEntity, Map.class);

            // ✅ Print FastAPI Response
            LOGGER.info("✅ FastAPI Response: " + response.getStatusCode() + " - " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && response.getBody().containsKey("employeeId")) {
                String employeeId = response.getBody().get("employeeId").toString();
                LOGGER.info("✅ Face recognized. Employee ID: " + employeeId);
                return attendanceService.markAttendance(employeeId);
            }

            LOGGER.warning("❌ Face not recognized!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Face not recognized!");

        } catch (IOException e) {
            LOGGER.severe("❌ Error processing image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error processing image: " + e.getMessage());
        }
    }
}
