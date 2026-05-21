package com.smartacademic.controller;

import com.smartacademic.dto.EvaluationDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.Equipment;
import com.smartacademic.entity.MentoringSession;
import com.smartacademic.entity.User;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.service.BorrowingService;
import com.smartacademic.service.EquipmentService;
import com.smartacademic.service.MentoringSessionService;
import com.smartacademic.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<MentoringSession> pendingSessions = mentoringSessionService.getPendingSessionsByLecturer(user.getId());
        List<MentoringSession> allSessions = mentoringSessionService.getAllSessionsByLecturer(user.getId());

        long pending   = allSessions.stream().filter(s -> s.getStatus() == SessionStatus.PENDING).count();
        long completed = allSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        long cancelled = allSessions.stream().filter(s -> s.getStatus() == SessionStatus.CANCELLED).count();

        model.addAttribute("user", user);
        model.addAttribute("pendingSessions", pendingSessions);
        model.addAttribute("allSessions", allSessions);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("completedCount", completed);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("totalCount", allSessions.size());
        model.addAttribute("activePage", "dashboard");
        return "lecturer/dashboard";
    }

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
            dto.setSessionFee(fullUser.getLecturerInfo().getSessionFee());
            if (fullUser.getLecturerInfo().getDepartment() != null) {
                dto.setDepartmentId(fullUser.getLecturerInfo().getDepartment().getId());
            }
        }
        model.addAttribute("user", fullUser);
        if (!model.containsAttribute("profileDTO")) {
            model.addAttribute("profileDTO", dto);
        }
        model.addAttribute("departments", userService.getAllDepartments());
        model.addAttribute("activePage", "profile");
        return "lecturer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileDTO") UserProfileDTO profileDTO,
                                BindingResult result,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", userService.getUserById(getCurrentUser(session).getId()));
            model.addAttribute("departments", userService.getAllDepartments());
            model.addAttribute("activePage", "profile");
            return "lecturer/profile";
        }
        User user = getCurrentUser(session);
        userService.updateProfile(user.getId(), profileDTO);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công");
        return "redirect:/lecturer/profile";
    }

    @GetMapping("/sessions/{id}/evaluate")
    public String evaluatePage(@PathVariable Long id, HttpSession session, Model model,
                               RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        MentoringSession sess = mentoringSessionService.getSessionById(id);

        if (!sess.getLecturer().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Bạn không có quyền đánh giá buổi tư vấn này");
            return "redirect:/lecturer/dashboard";
        }
        if (sess.getStatus() == SessionStatus.COMPLETED) {
            redirectAttributes.addFlashAttribute("errorMsg", "Buổi tư vấn này đã được đánh giá");
            return "redirect:/lecturer/dashboard";
        }

        List<Equipment> equipments = equipmentService.getAllActive();
        EvaluationDTO dto = new EvaluationDTO();
        dto.setSessionId(id);

        model.addAttribute("user", user);
        // Tránh dùng key "session" vì Thymeleaf đã reserve cho HttpSession.
        model.addAttribute("sess", sess);
        model.addAttribute("equipments", equipments);
        if (!model.containsAttribute("evaluationDTO")) {
            model.addAttribute("evaluationDTO", dto);
        }
        model.addAttribute("activePage", "dashboard");
        return "lecturer/evaluate";
    }

    @PostMapping("/sessions/{id}/evaluate")
    public String submitEvaluation(@PathVariable Long id,
                                   @Valid @ModelAttribute("evaluationDTO") EvaluationDTO evaluationDTO,
                                   BindingResult result,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        evaluationDTO.setSessionId(id);

        if (result.hasErrors()) {
            User user = getCurrentUser(session);
            model.addAttribute("sess", mentoringSessionService.getSessionById(id));
            model.addAttribute("equipments", equipmentService.getAllActive());
            model.addAttribute("user", user);
            model.addAttribute("activePage", "dashboard");
            model.addAttribute("errorMsg", "Vui lòng nhập điểm kỹ năng và thái độ từ 1 đến 10.");
            return "lecturer/evaluate";
        }

        try {
            User user = getCurrentUser(session);
            Long borrowingId = borrowingService.evaluateAndAssignEquipment(user.getId(), evaluationDTO);
            if (borrowingId != null) {
                redirectAttributes.addFlashAttribute("successMsg",
                        "Đánh giá thành công! Phiếu mượn #" + borrowingId
                                + " đã gửi Admin (Chờ cấp phát). Tồn kho sẽ trừ khi Admin bấm \"Xác nhận xuất kho\".");
            } else {
                redirectAttributes.addFlashAttribute("successMsg",
                        "Đánh giá thành công! (Chưa chỉ định thiết bị — không tạo phiếu mượn.)");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/lecturer/dashboard";
    }
}
