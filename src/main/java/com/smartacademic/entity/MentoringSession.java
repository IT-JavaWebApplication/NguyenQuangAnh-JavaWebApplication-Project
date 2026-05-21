package com.smartacademic.entity;

import com.smartacademic.enums.SessionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "mentoring_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_lecturer_slot",
                columnNames = {"lecturer_id", "session_date", "start_time"}
        ))
@Data
@NoArgsConstructor
public class MentoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AcademicEvaluation evaluation;

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BorrowingRecord borrowingRecord;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}