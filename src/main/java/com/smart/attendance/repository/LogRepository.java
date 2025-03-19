package com.smart.attendance.repository;

import com.smart.attendance.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
    List<Log> findAllByOrderByTimestampDesc(); // ✅ Retrieve logs sorted by timestamp
}
