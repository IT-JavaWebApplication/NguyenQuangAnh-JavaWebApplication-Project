package com.smartacademic.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Số buổi tư vấn theo tháng (12 tháng gần nhất) - dữ liệu cho biểu đồ line/bar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySessionDTO {
    private Integer year;
    private Integer month;
    private Long sessionCount;
    private Long completedCount;

    public String getLabel() {
        return String.format("%02d/%d", month, year);
    }
}
