package com.smartacademic.controller;

import com.smartacademic.dto.EquipmentDTO;
import com.smartacademic.entity.BorrowingRecord;
import com.smartacademic.entity.Equipment;
import com.smartacademic.entity.User;
import com.smartacademic.service.BorrowingService;
import com.smartacademic.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private BorrowingService borrowingService;

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    // GET /admin/dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<BorrowingRecord> pendingRecords = borrowingService.getPendingDispatch();
        List<Equipment> equipments = equipmentService.getAll();
        model.addAttribute("user", user);
        model.addAttribute("pendingRecords", pendingRecords);
        model.addAttribute("equipments", equipments);
        model.addAttribute("totalEquipments", equipments.size());
        model.addAttribute("pendingCount", pendingRecords.size());
        return "admin/dashboard";
    }

    // ===================== CORE-04: Quản lý Thiết bị =====================

    // GET /admin/equipment
    @GetMapping("/equipment")
    public String equipmentList(Model model, HttpSession session) {
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("equipments", equipmentService.getAll());
        model.addAttribute("equipmentDTO", new EquipmentDTO());
        return "admin/equipment";
    }

    // POST /admin/equipment/create
    @PostMapping("/equipment/create")
    public String createEquipment(@Valid @ModelAttribute EquipmentDTO equipmentDTO,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes,
                                  Model model,
                                  HttpSession session) {
        if (result.hasErrors()) {
            model.addAttribute("user", getCurrentUser(session));
            model.addAttribute("equipments", equipmentService.getAll());
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

    // GET /admin/equipment/{id}/edit
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
        dto.setIsActive(equipment.getIsActive());

        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("equipment", equipment);
        model.addAttribute("equipmentDTO", dto);
        return "admin/equipment-edit";
    }

    // POST /admin/equipment/{id}/edit
    @PostMapping("/equipment/{id}/edit")
    public String updateEquipment(@PathVariable Long id,
                                  @Valid @ModelAttribute EquipmentDTO equipmentDTO,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "admin/equipment-edit";
        try {
            equipmentService.update(id, equipmentDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật thiết bị thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    // POST /admin/equipment/{id}/delete
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

    // ===================== CORE-08: Cấp phát Thiết bị =====================

    // GET /admin/borrowing
    @GetMapping("/borrowing")
    public String borrowingList(Model model, HttpSession session) {
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("pendingRecords", borrowingService.getPendingDispatch());
        return "admin/borrowing";
    }

    // GET /admin/borrowing/{id}
    @GetMapping("/borrowing/{id}")
    public String borrowingDetail(@PathVariable Long id, Model model, HttpSession session) {
        BorrowingRecord record = borrowingService.getById(id);
        model.addAttribute("user", getCurrentUser(session));
        model.addAttribute("record", record);
        return "admin/borrowing-detail";
    }

    // POST /admin/borrowing/{id}/dispatch - CORE-08
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