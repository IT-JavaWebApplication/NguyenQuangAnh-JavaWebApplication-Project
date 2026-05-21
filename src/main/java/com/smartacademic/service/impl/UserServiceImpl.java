package com.smartacademic.service.impl;

import com.smartacademic.dto.LoginDTO;
import com.smartacademic.dto.RegisterDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.*;
import com.smartacademic.enums.Role;
import com.smartacademic.repository.UserRepository;
import com.smartacademic.service.UserService;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public User register(RegisterDTO dto) {
        // CORE-01: Validation
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (!dto.isPasswordMatch()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        // CORE-01: Hash password bằng BCrypt
        String hashedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12));

        User user = new User(dto.getUsername(), dto.getEmail(), hashedPassword, Role.STUDENT);
        userRepository.save(user);

        // Tạo profile
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone());
        profile.setStudentCode(dto.getStudentCode());
        profile.setClassName(dto.getClassName());
        sessionFactory.getCurrentSession().persist(profile);

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> login(LoginDTO dto) {
        // CORE-01: Xác thực đăng nhập
        Optional<User> userOpt = userRepository.findByUsername(dto.getUsername());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        // So sánh mật khẩu đã hash
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        // CORE-03: Cập nhật hồ sơ cá nhân
        User user = getUserById(userId);
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone());
        profile.setStudentCode(dto.getStudentCode());
        profile.setClassName(dto.getClassName());

        sessionFactory.getCurrentSession().merge(profile);

        // Nếu là giảng viên, cập nhật thêm thông tin
        if (user.getRole() == Role.LECTURER && dto.getDepartmentId() != null) {
            Lecturer lecturer = user.getLecturerInfo();
            if (lecturer != null) {
                lecturer.setSpecialization(dto.getSpecialization());
                lecturer.setBio(dto.getBio());
                Department dept = sessionFactory.getCurrentSession().get(Department.class, dto.getDepartmentId());
                if (dept != null) lecturer.setDepartment(dept);
                sessionFactory.getCurrentSession().merge(lecturer);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getLecturersByDepartment(Long departmentId) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT u FROM User u " +
                                "JOIN FETCH u.profile p " +
                                "JOIN u.lecturerInfo li " +
                                "WHERE li.department.id = :deptId AND u.isActive = true " +
                                "ORDER BY p.fullName ASC", User.class)
                .setParameter("deptId", departmentId)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Department d WHERE d.isActive = true ORDER BY d.name ASC", Department.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getLecturers() {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT u FROM User u " +
                                "JOIN FETCH u.profile " +
                                "WHERE u.role = 'LECTURER' AND u.isActive = true", User.class)
                .list();
    }
}