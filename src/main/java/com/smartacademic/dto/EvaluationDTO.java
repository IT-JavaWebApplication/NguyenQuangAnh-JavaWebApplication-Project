package com.smartacademic.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

@Data
public class EvaluationDTO {

    @NotNull
    private Long sessionId;

    @NotNull(message = "Điểm kỹ năng không được để trống")
    @Min(value = 1, message = "Điểm tối thiểu là 1")
    @Max(value = 10, message = "Điểm tối đa là 10")
    private Integer skillScore;

    @NotNull(message = "Điểm thái độ không được để trống")
    @Min(value = 1, message = "Điểm tối thiểu là 1")
    @Max(value = 10, message = "Điểm tối đa là 10")
    private Integer attitudeScore;

    private String comments;
    private String recommendations;

    // Map<equipmentId, quantity> - danh sách thiết bị chỉ định
    private List<Long> equipmentIds;
    private List<Integer> equipmentQuantities;
}