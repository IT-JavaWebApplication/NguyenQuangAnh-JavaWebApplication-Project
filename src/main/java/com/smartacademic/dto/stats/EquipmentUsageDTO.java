package com.smartacademic.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


 //thiết bị được mượn nhiều nhất.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUsageDTO {
    private Long equipmentId;
    private String code;
    private String name;
    private Long totalBorrowed;
    private Integer available;
    private Integer quantity;
}
