package com.smartacademic.service;

import com.smartacademic.dto.stats.DashboardStatsDTO;
import com.smartacademic.dto.stats.EquipmentUsageDTO;
import com.smartacademic.dto.stats.MonthlySessionDTO;
import com.smartacademic.dto.stats.TopLecturerDTO;

import java.util.List;

public interface StatisticsService {
    DashboardStatsDTO getDashboardStats();
    List<TopLecturerDTO> getTopLecturers(int limit);
    List<EquipmentUsageDTO> getTopBorrowedEquipments(int limit);
    List<MonthlySessionDTO> getSessionsByMonth(int monthsBack);
}
