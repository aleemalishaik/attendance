package com.smart.attendance.repository;

import com.smart.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmployeeId(String employeeId);

    Optional<User> findByEmail(String email);

    Optional<User> findByNameIgnoreCase(String name); // ✅ Added method to find users by name (case-insensitive)

    List<User> findAllByOrderByEmployeeIdAsc();
}
