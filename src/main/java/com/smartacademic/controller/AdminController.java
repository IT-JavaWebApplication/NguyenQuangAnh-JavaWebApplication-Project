package com.smartacademic.controller;

import com.smartacademic.dto.EquipmentDTO;
import com.smartacademic.dto.stats.DashboardStatsDTO;
import com.smartacademic.dto.stats.EquipmentUsageDTO;
import com.smartacademic.dto.stats.MonthlySessionDTO;
import com.smartacademic.dto.stats.TopLecturerDTO;
import com.smartacademic.entity.BorrowingRecord;
import com.smartacademic.entity.Equipment;
import com.smartacademic.entity.User;
import com.smartacademic.service.BorrowingService;
import com.smartacademic.service.EquipmentService;
import com.smartacademic.service.StatisticsService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private BorrowingService borrowingService;

    @Autowired
    private StatisticsService statisticsService;

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        DashboardStatsDTO stats = statisticsService.getDashboardStats();
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("stats", stats);
        model.addAttribute("activePage", "dashboard");
        addChartData(model, stats);
        return "admin/dashboard";
    }

    @GetMapping("/statistics")
    public String statistics(HttpSession session, Model model) {
        DashboardStatsDTO stats = statisticsService.getDashboardStats();
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("stats", stats);
        model.addAttribute("activePage", "statistics");
        addChartData(model, stats);
        return "admin/statistics";
    }

    private void addChartData(Model model, DashboardStatsDTO stats) {
        List<MonthlySessionDTO> months = stats.getSessionsByMonth() != null
                ? stats.getSessionsByMonth() : List.of();
        model.addAttribute("chartMonthlyLabels",
                months.stream().map(MonthlySessionDTO::getLabel).collect(Collectors.toList()));
        model.addAttribute("chartMonthlyTotal",
                months.stream().map(MonthlySessionDTO::getSessionCount).collect(Collectors.toList()));
        model.addAttribute("chartMonthlyDone",
                months.stream().map(MonthlySessionDTO::getCompletedCount).collect(Collectors.toList()));

        List<TopLecturerDTO> lecturers = stats.getTopLecturers() != null
                ? stats.getTopLecturers() : List.of();
        model.addAttribute("chartLecturerNames",
                lecturers.stream().map(TopLecturerDTO::getLecturerName).collect(Collectors.toList()));
        model.addAttribute("chartLecturerCounts",
                lecturers.stream().map(TopLecturerDTO::getSessionCount).collect(Collectors.toList()));

        List<EquipmentUsageDTO> equipments = stats.getTopEquipments() != null
                ? stats.getTopEquipments() : List.of();
        model.addAttribute("chartEquipmentNames",
                equipments.stream().map(EquipmentUsageDTO::getName).collect(Collectors.toList()));
        model.addAttribute("chartEquipmentTotals",
                equipments.stream().map(EquipmentUsageDTO::getTotalBorrowed).collect(Collectors.toList()));
    }

    @GetMapping("/equipment")
    public String equipmentList(Model model, HttpSession session) {
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("equipments", equipmentService.getAll());
        if (!model.containsAttribute("equipmentDTO")) {
            model.addAttribute("equipmentDTO", new EquipmentDTO());
        }
        model.addAttribute("activePage", "equipment");
        return "admin/equipment";
    }

    @PostMapping("/equipment/create")
    public String createEquipment(@Valid @ModelAttribute("equipmentDTO") EquipmentDTO equipmentDTO,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes,
                                  Model model,
                                  HttpSession session) {
        if (result.hasErrors()) {
            model.addAttribute("user", getCurrentUser(session));
            model.addAttribute("equipments", equipmentService.getAll());
            model.addAttribute("openAddModal", true);
            return "admin/equipment";
        }
        try {
            equipmentService.create(equipmentDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Thêm thiết bị thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    @GetMapping("/equipment/{id}/edit")
    public String editEquipmentPage(@PathVariable Long id, Model model, HttpSession session) {
        Equipment equipment = equipmentService.getById(id);
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setCode(equipment.getCode());
        dto.setName(equipment.getName());
        dto.setDescription(equipment.getDescription());
        dto.setQuantity(equipment.getQuantity());
        dto.setAvailable(equipment.getAvailable());
        dto.setUnit(equipment.getUnit());
        dto.setDepositAmount(equipment.getDepositAmount());
        dto.setIsActive(equipment.getIsActive());

        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("equipment", equipment);
        model.addAttribute("equipmentDTO", dto);
        model.addAttribute("activePage", "equipment");
        return "admin/equipment-edit";
    }

    @PostMapping("/equipment/{id}/edit")
    public String updateEquipment(@PathVariable Long id,
                                  @Valid @ModelAttribute("equipmentDTO") EquipmentDTO equipmentDTO,
                                  BindingResult result,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", getCurrentUser(session));
            model.addAttribute("equipment", equipmentService.getById(id));
            model.addAttribute("activePage", "equipment");
            return "admin/equipment-edit";
        }
        try {
            equipmentService.update(id, equipmentDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật thiết bị thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    @PostMapping("/equipment/{id}/delete")
    public String deleteEquipment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            equipmentService.delete(id);
            redirectAttributes.addFlashAttribute("successMsg", "Xóa thiết bị thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    @GetMapping("/borrowing")
    public String borrowingList(Model model, HttpSession session) {
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("pendingRecords", borrowingService.getPendingDispatch());
        model.addAttribute("activePage", "borrowing");
        return "admin/borrowing";
    }

    @GetMapping("/borrowing/{id}")
    public String borrowingDetail(@PathVariable Long id, Model model, HttpSession session) {
        BorrowingRecord record = borrowingService.getById(id);
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("record", record);
        model.addAttribute("activePage", "borrowing");
        return "admin/borrowing-detail";
    }

    @PostMapping("/borrowing/{id}/dispatch")
    public String confirmDispatch(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentUser(session);
            borrowingService.confirmDispatch(id, admin.getId());
            redirectAttributes.addFlashAttribute("successMsg",
                    "Xác nhận xuất kho thành công! Tồn kho đã được cập nhật.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
        }
        return "redirect:/admin/borrowing";
    }
}
