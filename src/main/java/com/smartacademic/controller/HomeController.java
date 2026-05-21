package com.smartacademic.controller;

import com.smartacademic.entity.User;
import com.smartacademic.enums.Role;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (user.getRole() == Role.ADMIN)    return "redirect:/admin/dashboard";
        if (user.getRole() == Role.LECTURER) return "redirect:/lecturer/dashboard";
        return "redirect:/student/dashboard";
    }
}
