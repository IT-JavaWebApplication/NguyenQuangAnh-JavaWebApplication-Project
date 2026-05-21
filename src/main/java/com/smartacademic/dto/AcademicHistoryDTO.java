package com.smartacademic.dto;

import com.smartacademic.enums.SessionStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO cho CORE-07: Hồ sơ học tập liên kết (kết quả JOIN phức tạp)
 */
@Data
public class AcademicHistoryDTO {

    // Thông tin buổi tư vấn
    private Long sessionId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionStatus status;
    private String note;

    // Thông tin giảng viên
    private Long lecturerId;
    private String lecturerName;
    private String lecturerCode;
    private String department;
    private String specialization;

    // Đánh giá năng lực
    private Integer skillScore;
    private Integer attitudeScore;
    private String comments;
    private String recommendations;

    // Danh sách thiết bị đã mượn
    private List<BorrowedEquipmentDTO> borrowedEquipments;
    private String borrowingStatus;

    @Data
    public static class BorrowedEquipmentDTO {
        private String equipmentCode;
        private String equipmentName;
        private Integer quantity;
        private String unit;
    }
}