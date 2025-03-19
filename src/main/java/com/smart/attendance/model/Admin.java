package com.smart.attendance.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true) // ✅ Added email field
    private String email;

    @Column(name = "password_hash", nullable = false) // ✅ Matches DB column
    private String password;

    @Column(name = "is_super_admin", nullable = false)
    private boolean isSuperAdmin;
    
    @JsonIgnore
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isSuperAdmin() {
        return isSuperAdmin;
    }

    // ✅ Format createdAt as hh:mm AM/PM DD/MM/YYYY
    @JsonProperty("createdAtFormatted")
    public String getFormattedCreatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a dd/MM/yyyy");
        return createdAt != null ? createdAt.format(formatter) : "N/A";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // Auto-set timestamp on creation
    }
}
