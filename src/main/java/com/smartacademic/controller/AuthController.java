package com.smartacademic.controller;

import com.smartacademic.dto.LoginDTO;
import com.smartacademic.dto.RegisterDTO;
import com.smartacademic.entity.User;
import com.smartacademic.enums.Role;
import com.smartacademic.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    // GET /auth/login
    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session,
                            @RequestParam(required = false) String error) {

        if (session.getAttribute("currentUser") != null) {
            return redirectByRole((User) session.getAttribute("currentUser"));
        }
        model.addAttribute("loginDTO", new LoginDTO());
        if ("unauthorized".equals(error)) {
            model.addAttribute("errorMsg", "Vui lòng đăng nhập để tiếp tục");
        }
        return "auth/login";
    }

    // POST /auth/login
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginDTO loginDTO,
                        BindingResult result,
                        HttpSession session,
                        Model model) {
        if (result.hasErrors()) {
            return "auth/login";
        }

        Optional<User> userOpt = userService.login(loginDTO);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Tên đăng nhập hoặc mật khẩu không đúng");
            model.addAttribute("loginDTO", loginDTO);
            return "auth/login";
        }

        User user = userOpt.get();
        session.setAttribute("currentUser", user);
        session.setMaxInactiveInterval(30 * 60); // 30 phút

        log.info("User {} logged in with role {}", user.getUsername(), user.getRole());
        return redirectByRole(user);
    }

    // GET /auth/register
    @GetMapping("/register")
    public String registerPage(Model model, HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return redirectByRole((User) session.getAttribute("currentUser"));
        }
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    // POST /auth/register
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterDTO registerDTO,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(registerDTO);
            redirectAttributes.addFlashAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("registerDTO", registerDTO);
            return "auth/register";
        }
    }

    // GET /auth/logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login?logout=true";
    }

    // GET /auth/access-denied
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "common/403";
    }

    private String redirectByRole(User user) {
        if (user.getRole() == Role.ADMIN) return "redirect:/admin/dashboard";
        if (user.getRole() == Role.LECTURER) return "redirect:/lecturer/dashboard";
        return "redirect:/student/dashboard";
    }
}