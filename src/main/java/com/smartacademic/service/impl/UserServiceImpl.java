package com.smartacademic.service.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.dto.LoginDTO;
import com.smartacademic.dto.RegisterDTO;
import com.smartacademic.dto.UserProfileDTO;
import com.smartacademic.entity.Department;
import com.smartacademic.entity.Lecturer;
import com.smartacademic.entity.User;
import com.smartacademic.entity.UserProfile;
import com.smartacademic.enums.Role;
import com.smartacademic.repository.UserRepository;
import com.smartacademic.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private HibernateSessionProvider sessionFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (!dto.isPasswordMatch()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        User user = new User(dto.getUsername(), dto.getEmail(), hashedPassword, Role.STUDENT);
        userRepository.save(user);

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
        Optional<User> userOpt = userRepository.findByUsername(dto.getUsername());
        if (userOpt.isEmpty()) return Optional.empty();
        User user = userOpt.get();
        return passwordEncoder.matches(dto.getPassword(), user.getPassword()) ? Optional.of(user) : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        User user = getUserById(userId);
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone());
        if (user.getRole() == Role.STUDENT) {
            profile.setStudentCode(dto.getStudentCode());
            profile.setClassName(dto.getClassName());
        }
        sessionFactory.getCurrentSession().merge(profile);

        if (user.getRole() == Role.LECTURER) {
            Lecturer lecturer = user.getLecturerInfo();
            if (lecturer != null) {
                lecturer.setSpecialization(dto.getSpecialization());
                lecturer.setBio(dto.getBio());
                if (dto.getSessionFee() != null) {
                    lecturer.setSessionFee(dto.getSessionFee());
                }
                if (dto.getDepartmentId() != null) {
                    Department dept = sessionFactory.getCurrentSession().get(Department.class, dto.getDepartmentId());
                    if (dept != null) lecturer.setDepartment(dept);
                }
                sessionFactory.getCurrentSession().merge(lecturer);
            }
        }
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.update(user);
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
