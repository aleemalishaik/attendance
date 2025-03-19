package com.smart.attendance.controller;

import com.smart.attendance.model.Log;
import com.smart.attendance.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "http://localhost:3000")
public class LogController {

    @Autowired
    private LogRepository logRepository;

    // ✅ Fetch all logs (latest first)
    @GetMapping("/all")
    public List<Log> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }

    // ✅ Export logs as CSV (with Authorization)
    @GetMapping("/export-csv")
    public ResponseEntity<String> exportLogsAsCSV(@RequestHeader("Authorization") String token, HttpServletResponse response) {
        // TODO: Validate token if necessary (implement authentication logic here)
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

        try {
            List<Log> logs = logRepository.findAllByOrderByTimestampDesc();

            StringWriter writer = new StringWriter();
            PrintWriter csvWriter = new PrintWriter(writer);

            // CSV Headers
            csvWriter.println("ID,Timestamp,Action,Details");

            // CSV Data
            for (Log log : logs) {
                csvWriter.println(log.getId() + "," +
                        log.getTimestamp() + "," +
                        log.getAction() + "," +
                        (log.getDetails() != null ? log.getDetails() : "N/A"));
            }

            csvWriter.flush();
            csvWriter.close();

            // Set response headers for CSV download
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=activity_logs.csv");
            headers.add("Content-Type", "text/csv");

            return new ResponseEntity<>(writer.toString(), headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating CSV");
        }
    }
}
