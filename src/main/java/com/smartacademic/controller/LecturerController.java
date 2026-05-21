package com.smartacademic.controller;

import com.smartacademic.dto.EvaluationDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.Equipment;
import com.smartacademic.entity.MentoringSession;
import com.smartacademic.entity.User;
import com.smartacademic.service.BorrowingService;
import com.smartacademic.service.EquipmentService;
import com.smartacademic.service.MentoringSessionService;
import com.smartacademic.service.UserService;
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
@RequestMapping("/lecturer")
public class LecturerController {

    @Autowired
    private MentoringSessionService mentoringSessionService;

    @Autowired
    private BorrowingService borrowingService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    // GET /lecturer/dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<MentoringSession> pendingSessions = mentoringSessionService.getPendingSessionsByLecturer(user.getId());
        List<MentoringSession> allSessions = mentoringSessionService.getAllSessionsByLecturer(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("pendingSessions", pendingSessions);
        model.addAttribute("allSessions", allSessions);
        return "lecturer/dashboard";
    }

    // GET /lecturer/profile - CORE-03
    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        User fullUser = userService.getUserById(user.getId());
        UserProfileDTO dto = new UserProfileDTO();
        if (fullUser.getProfile() != null) {
            dto.setFullName(fullUser.getProfile().getFullName());
            dto.setPhone(fullUser.getProfile().getPhone());
        }
        if (fullUser.getLecturerInfo() != null) {
            dto.setSpecialization(fullUser.getLecturerInfo().getSpecialization());
            dto.setBio(fullUser.getLecturerInfo().getBio());
            if (fullUser.getLecturerInfo().getDepartment() != null) {
                dto.setDepartmentId(fullUser.getLecturerInfo().getDepartment().getId());
            }
        }
        model.addAttribute("user", fullUser);
        model.addAttribute("profileDTO", dto);
        model.addAttribute("departments", userService.getAllDepartments());
        return "lecturer/profile";
    }

    // POST /lecturer/profile - CORE-03
    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute UserProfileDTO profileDTO,
                                BindingResult result,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "lecturer/profile";
        User user = getCurrentUser(session);
        userService.updateProfile(user.getId(), profileDTO);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công");
        return "redirect:/lecturer/profile";
    }

    // GET /lecturer/sessions/{id}/evaluate - CORE-06
    @GetMapping("/sessions/{id}/evaluate")
    public String evaluatePage(@PathVariable Long id, HttpSession session, Model model) {
        User user = getCurrentUser(session);
        MentoringSession sess = mentoringSessionService.getSessionById(id);

        // Kiểm tra quyền
        if (!sess.getLecturer().getId().equals(user.getId())) {
            return "redirect:/lecturer/dashboard";
        }

        List<Equipment> equipments = equipmentService.getAllActive();
        model.addAttribute("user", user);
        model.addAttribute("session", sess);
        model.addAttribute("equipments", equipments);
        model.addAttribute("evaluationDTO", new EvaluationDTO());
        return "lecturer/evaluate";
    }

    // POST /lecturer/sessions/{id}/evaluate - CORE-06 (Transaction)
    @PostMapping("/sessions/{id}/evaluate")
    public String submitEvaluation(@PathVariable Long id,
                                   @Valid @ModelAttribute EvaluationDTO evaluationDTO,
                                   BindingResult result,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        evaluationDTO.setSessionId(id);

        if (result.hasErrors()) {
            User user = getCurrentUser(session);
            model.addAttribute("session", mentoringSessionService.getSessionById(id));
            model.addAttribute("equipments", equipmentService.getAllActive());
            model.addAttribute("user", user);
            return "lecturer/evaluate";
        }

        try {
            User user = getCurrentUser(session);
            borrowingService.evaluateAndAssignEquipment(user.getId(), evaluationDTO);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đánh giá thành công! Phiếu mượn thiết bị đã được tạo và chuyển sang trạng thái chờ cấp phát.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/lecturer/dashboard";
    }
}