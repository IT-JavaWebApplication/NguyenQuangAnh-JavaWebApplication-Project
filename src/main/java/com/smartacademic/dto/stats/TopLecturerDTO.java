package com.smartacademic.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Top giảng viên theo số buổi tư vấn. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopLecturerDTO {
    private Long lecturerId;
    private String lecturerName;
    private String department;
    private Long sessionCount;
    private Double avgSkillScore;
}
