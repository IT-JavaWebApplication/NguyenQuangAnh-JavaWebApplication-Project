package com.smartacademic.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lecturers")
@Data
@NoArgsConstructor
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "lecturer_code", unique = true, length = 20)
    private String lecturerCode;

    @Column(length = 200)
    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String bio;

    /**
     * Phí cho 1 buổi tư vấn (VND). 0 = miễn phí.
     * Sinh viên cần thanh toán phí này khi đặt lịch.
     */
    @Column(name = "session_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal sessionFee = BigDecimal.ZERO;
}
