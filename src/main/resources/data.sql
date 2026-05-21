
INSERT IGNORE INTO lab_room_types (code, name, capacity, description) VALUES
                                                                          ('LAB_LT',    'Phòng Lab Lập trình',   30, 'Phòng thực hành lập trình với máy tính'),
                                                                          ('LAB_MANG',  'Phòng Lab Mạng',        24, 'Phòng thực hành hệ thống mạng và thiết bị'),
                                                                          ('LAB_DT',    'Phòng Lab Điện tử',     20, 'Phòng thực hành điện tử và vi mạch'),
                                                                          ('LAB_AI',    'Phòng Lab AI/ML',       25, 'Phòng thực hành trí tuệ nhân tạo'),
                                                                          ('LAB_AN',    'Phòng Lab Bảo mật',     20, 'Phòng thực hành an toàn thông tin');

-- Admin mặc định (password: Admin@123)
INSERT IGNORE INTO users (username, email, password, role) VALUES
    ('admin', 'admin@smartacademic.edu.vn',
     '$2a$12$9sAq0U5..kWCNM7ZTaEixO8O6wXqjVs0j3ZaLFr3v.mIxaLOAH9nG', 'ADMIN');

-- Thêm chữ IGNORE vào sau INSERT để nếu trùng id = 1, MySQL sẽ tự bỏ qua thay vì báo lỗi sập app
INSERT IGNORE INTO departments (id, code, name, description, is_active)
VALUES (1, 'CNTT', 'Công nghệ thông tin', 'Khoa Công nghệ thông tin & Truyền thông', 1);

INSERT IGNORE INTO users (id, username, email, password, role, is_active, created_at, updated_at)
VALUES (1, 'giangvien01', 'lecturer01@smartacademic.edu.vn', 'password123', 'LECTURER', 1, NOW(), NOW());

INSERT IGNORE INTO users (id, username, email, password, role, is_active, created_at, updated_at)
VALUES (2, 'sinhvien01', 'student01@smartacademic.edu.vn', 'password123', 'STUDENT', 1, NOW(), NOW());

INSERT IGNORE INTO user_profiles (id, user_id, full_name, phone, avatar_url, student_code, class_name, created_at, updated_at)
VALUES (1, 1, 'Nguyễn Văn A', '0901234567', 'https://example.com/avatars/lecturer01.png', NULL, NULL, NOW(), NOW());

INSERT IGNORE INTO user_profiles (id, user_id, full_name, phone, avatar_url, student_code, class_name, created_at, updated_at)
VALUES (2, 2, 'Trần Thị B', '0987654321', 'https://example.com/avatars/student01.png', 'SV20261102', 'K26-CNTT01', NOW(), NOW());

INSERT IGNORE INTO lecturers (id, user_id, department_id, lecturer_code, specialization, bio)
VALUES (1, 1, 1, 'GV2026001', 'Trí tuệ nhân tạo & Học máy', 'Phó giáo sư, Tiến sĩ chuyên ngành Khoa học máy tính với hơn 10 năm kinh nghiệm nghiên cứu.');