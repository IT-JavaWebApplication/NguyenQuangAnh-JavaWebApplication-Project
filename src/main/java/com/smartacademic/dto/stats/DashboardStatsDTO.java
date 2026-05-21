package com.smartacademic.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//Tổng hợp số liệu cho Admin Dashboard.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalUsers;
    private long totalStudents;
    private long totalLecturers;
    private long totalEquipments;
    //Tổng số thiết bị (theo quantity) đang được mượn.
    private long totalEquipmentsBorrowed;
    private long pendingDispatchCount;
    private long overdueCount;
    private long totalSessions;
    private long completedSessions;
    private long pendingSessions;
    private double avgSkillScore;
    private double avgAttitudeScore;
    private List<TopLecturerDTO> topLecturers;
    private List<EquipmentUsageDTO> topEquipments;
    private List<MonthlySessionDTO> sessionsByMonth;
}
