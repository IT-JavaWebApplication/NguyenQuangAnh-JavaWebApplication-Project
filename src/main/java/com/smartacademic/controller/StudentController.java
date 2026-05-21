package com.smartacademic.controller;

import com.smartacademic.dto.AcademicHistoryDTO;
import com.smartacademic.dto.BookingRequestDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.Department;
import com.smartacademic.entity.MentoringSession;
import com.smartacademic.entity.User;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private UserService userService;

    @Autowired
    private MentoringSessionService mentoringSessionService;

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<MentoringSession> sessions = mentoringSessionService.getSessionsByStudent(user.getId());

        long total     = sessions.size();
        long pending   = sessions.stream().filter(s -> s.getStatus().name().equals("PENDING")).count();
        long completed = sessions.stream().filter(s -> s.getStatus().name().equals("COMPLETED")).count();
        long cancelled = sessions.stream().filter(s -> s.getStatus().name().equals("CANCELLED")).count();

        model.addAttribute("user", user);
        model.addAttribute("sessions", sessions);
        model.addAttribute("totalCount", total);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("completedCount", completed);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("activePage", "dashboard");
        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        User fullUser = userService.getUserById(user.getId());
        UserProfileDTO dto = new UserProfileDTO();
        if (fullUser.getProfile() != null) {
            dto.setFullName(fullUser.getProfile().getFullName());
            dto.setPhone(fullUser.getProfile().getPhone());
            dto.setStudentCode(fullUser.getProfile().getStudentCode());
            dto.setClassName(fullUser.getProfile().getClassName());
        }
        model.addAttribute("user", fullUser);
        if (!model.containsAttribute("profileDTO")) {
            model.addAttribute("profileDTO", dto);
        }
        model.addAttribute("activePage", "profile");
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileDTO") UserProfileDTO profileDTO,
                                BindingResult result,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", userService.getUserById(getCurrentUser(session).getId()));
            model.addAttribute("activePage", "profile");
            return "student/profile";
        }
        User user = getCurrentUser(session);
        userService.updateProfile(user.getId(), profileDTO);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công");
        return "redirect:/student/profile";
    }

    @GetMapping("/booking")
    public String bookingPage(HttpSession session, Model model,
                              @RequestParam(required = false) Long departmentId,
                              @RequestParam(required = false) Long lecturerId) {
        User user = getCurrentUser(session);
        List<Department> departments = userService.getAllDepartments();
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setDepartmentId(departmentId);
        dto.setLecturerId(lecturerId);

        model.addAttribute("user", user);
        model.addAttribute("departments", departments);
        if (!model.containsAttribute("bookingDTO")) {
            model.addAttribute("bookingDTO", dto);
        }
        if (departmentId != null) {
            List<User> lecturers = userService.getLecturersByDepartment(departmentId);
            model.addAttribute("lecturers", lecturers);
            model.addAttribute("selectedDeptId", departmentId);
        }
        model.addAttribute("activePage", "booking");
        return "student/booking";
    }

    @PostMapping("/booking")
    public String submitBooking(@Valid @ModelAttribute("bookingDTO") BookingRequestDTO bookingDTO,
                                BindingResult result,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            User user = getCurrentUser(session);
            model.addAttribute("user", user);
            model.addAttribute("departments", userService.getAllDepartments());
            if (bookingDTO.getDepartmentId() != null) {
                model.addAttribute("lecturers", userService.getLecturersByDepartment(bookingDTO.getDepartmentId()));
                model.addAttribute("selectedDeptId", bookingDTO.getDepartmentId());
            }
            model.addAttribute("activePage", "booking");
            return "student/booking";
        }
        try {
            User user = getCurrentUser(session);
            mentoringSessionService.book(user.getId(), bookingDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Đặt lịch tư vấn thành công! Email xác nhận đã được gửi.");
            return "redirect:/student/dashboard";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            String redirect = "redirect:/student/booking";
            if (bookingDTO.getDepartmentId() != null) {
                redirect += "?departmentId=" + bookingDTO.getDepartmentId();
            }
            return redirect;
        }
    }

    @PostMapping("/sessions/{id}/cancel")
    public String cancelSession(@PathVariable Long id,
                                @RequestParam(required = false) String cancelReason,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(session);
            mentoringSessionService.cancel(id, user.getId(), cancelReason);
            redirectAttributes.addFlashAttribute("successMsg", "Hủy lịch thành công. Khung giờ đã được giải phóng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/student/dashboard";
    }

    @GetMapping("/history")
    public String academicHistory(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<AcademicHistoryDTO> history = mentoringSessionService.getAcademicHistory(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        model.addAttribute("activePage", "history");
        return "student/history";
    }

    // AJAX cho dynamic dropdown
    @GetMapping("/lecturers-by-department")
    @ResponseBody
    public List<Map<String, Object>> getLecturersByDept(@RequestParam Long departmentId) {
        List<User> lecturers = userService.getLecturersByDepartment(departmentId);
        return lecturers.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getProfile() != null ? u.getProfile().getFullName() : u.getUsername());
            m.put("email", u.getEmail());
            return m;
        }).toList();
    }
}
