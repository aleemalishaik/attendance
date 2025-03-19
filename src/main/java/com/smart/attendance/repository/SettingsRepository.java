package com.smart.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smart.attendance.model.Settings;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Integer> {
}
