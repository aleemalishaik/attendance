package com.smart.attendance.controller;

import com.smart.attendance.model.Log;
import com.smart.attendance.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogRepository logRepository;

    // ✅ Fetch all logs (latest first)
    @GetMapping("/all")
    public List<Log> getAllLogs() {
        return logRepository.findAllByOrderByTimestampDesc();
    }
}
