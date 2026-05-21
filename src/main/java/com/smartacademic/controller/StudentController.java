package com.smartacademic.controller;

import com.smartacademic.dto.AcademicHistoryDTO;
import com.smartacademic.dto.BookingRequestDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.Department;
import com.smartacademic.entity.MentoringSession;
import com.smartacademic.entity.User;
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
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private UserService userService;

    @Autowired
    private MentoringSessionService mentoringSessionService;

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    // GET /student/dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<MentoringSession> sessions = mentoringSessionService.getSessionsByStudent(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("sessions", sessions);
        return "student/dashboard";
    }

    // GET /student/profile - CORE-03
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
        model.addAttribute("profileDTO", dto);
        return "student/profile";
    }

    // POST /student/profile - CORE-03
    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute UserProfileDTO profileDTO,
                                BindingResult result,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "student/profile";
        }
        User user = getCurrentUser(session);
        userService.updateProfile(user.getId(), profileDTO);
        redirectAttributes.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công");
        return "redirect:/student/profile";
    }

    // GET /student/booking - CORE-05
    @GetMapping("/booking")
    public String bookingPage(HttpSession session, Model model,
                              @RequestParam(required = false) Long departmentId) {
        User user = getCurrentUser(session);
        List<Department> departments = userService.getAllDepartments();
        model.addAttribute("user", user);
        model.addAttribute("departments", departments);
        model.addAttribute("bookingDTO", new BookingRequestDTO());

        if (departmentId != null) {
            List<User> lecturers = userService.getLecturersByDepartment(departmentId);
            model.addAttribute("lecturers", lecturers);
            model.addAttribute("selectedDeptId", departmentId);
        }
        return "student/booking";
    }

    // POST /student/booking - CORE-05
    @PostMapping("/booking")
    public String submitBooking(@Valid @ModelAttribute BookingRequestDTO bookingDTO,
                                BindingResult result,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("user", getCurrentUser(session));
            model.addAttribute("departments", userService.getAllDepartments());
            return "student/booking";
        }
        try {
            User user = getCurrentUser(session);
            mentoringSessionService.book(user.getId(), bookingDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Đặt lịch tư vấn thành công! Vui lòng chờ xác nhận.");
            return "redirect:/student/dashboard";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/student/booking";
        }
    }

    // POST /student/sessions/{id}/cancel - CORE-09
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

    // GET /student/history - CORE-07
    @GetMapping("/history")
    public String academicHistory(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        List<AcademicHistoryDTO> history = mentoringSessionService.getAcademicHistory(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        return "student/history";
    }

    // AJAX: GET /student/lecturers-by-department?departmentId=X
    @GetMapping("/lecturers-by-department")
    @ResponseBody
    public List<User> getLecturersByDept(@RequestParam Long departmentId) {
        return userService.getLecturersByDepartment(departmentId);
    }
}