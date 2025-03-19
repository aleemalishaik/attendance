package com.smart.attendance.service;

import com.smart.attendance.model.Log;
import com.smart.attendance.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    @Autowired
    private LogRepository logRepository;

    public void logActivity(String action, String details) {
        Log log = new Log(action, details);
        logRepository.save(log);
    }
}
