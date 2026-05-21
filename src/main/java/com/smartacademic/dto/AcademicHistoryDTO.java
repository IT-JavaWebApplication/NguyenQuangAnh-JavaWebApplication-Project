package com.smartacademic.dto;

import com.smartacademic.enums.SessionStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Một mục trong hồ sơ học tập của sinh viên (kết quả JOIN phức tạp). */
@Data
public class AcademicHistoryDTO {

    private Long sessionId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionStatus status;
    private String note;

    private Long lecturerId;
    private String lecturerName;
    private String lecturerCode;
    private String department;
    private String specialization;

    private Integer skillScore;
    private Integer attitudeScore;
    private String comments;
    private String recommendations;

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