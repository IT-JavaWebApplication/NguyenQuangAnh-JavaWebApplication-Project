package com.smartacademic.filter;

import com.smartacademic.entity.User;
import com.smartacademic.enums.Role;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * CORE-02: Kiểm soát truy cập dựa trên role
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());

        // Lấy thông tin user từ session
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Chưa đăng nhập
        if (currentUser == null) {
            response.sendRedirect(contextPath + "/auth/login?error=unauthorized");
            return;
        }

        // CORE-02: Kiểm tra quyền theo role
        if (path.startsWith("/student/") && currentUser.getRole() != Role.STUDENT) {
            response.sendRedirect(contextPath + "/auth/access-denied");
            return;
        }
        if (path.startsWith("/lecturer/") && currentUser.getRole() != Role.LECTURER) {
            response.sendRedirect(contextPath + "/auth/access-denied");
            return;
        }
        if (path.startsWith("/admin/") && currentUser.getRole() != Role.ADMIN) {
            response.sendRedirect(contextPath + "/auth/access-denied");
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}