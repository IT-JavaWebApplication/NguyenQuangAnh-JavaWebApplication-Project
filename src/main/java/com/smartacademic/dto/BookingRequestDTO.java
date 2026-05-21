package com.smartacademic.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequestDTO {

    @NotNull(message = "Vui lòng chọn giảng viên")
    private Long lecturerId;

    @NotNull(message = "Vui lòng chọn ngày")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate sessionDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "Vui lòng chọn giờ kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    private String note;

    private Long departmentId;
}