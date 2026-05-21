package com.smartacademic.service;

import com.smartacademic.dto.LoginDTO;
import com.smartacademic.dto.RegisterDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.Department;
import com.smartacademic.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(RegisterDTO dto);
    Optional<User> login(LoginDTO dto);
    User getUserById(Long id);
    void updateProfile(Long userId, UserProfileDTO dto);
    void changePassword(Long userId, String currentPassword, String newPassword);
    List<User> getLecturersByDepartment(Long departmentId);
    List<Department> getAllDepartments();
    List<User> getLecturers();
}
