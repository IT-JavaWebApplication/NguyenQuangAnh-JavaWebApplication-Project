DROP DATABASE IF EXISTS smart_academic_db;
CREATE DATABASE IF NOT EXISTS smart_academic_db;
USE smart_academic_db;

-- Bảng khoa / ngành (Departments)
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active TINYINT(1) DEFAULT 1
);

-- Bảng tài khoản người dùng (Users)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- Mật khẩu đã mã hóa băm
    role ENUM('STUDENT', 'LECTURER', 'ADMIN') NOT NULL,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng hồ sơ cá nhân (User Profiles)
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(15),
    student_code VARCHAR(50) NULL, -- Dành cho sinh viên
    class_name VARCHAR(50) NULL,   -- Dành cho sinh viên
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng thông tin chi tiết giảng viên (Lecturers)
CREATE TABLE lecturers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    department_id BIGINT NOT NULL,
    specialization VARCHAR(255),
    bio TEXT,
    session_fee DECIMAL(15,2) DEFAULT 0.00, -- Thù lao tiết dạy phụ trợ nghiệp vụ
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- Bảng loại phòng Lab (Lab Room Types)
CREATE TABLE lab_room_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL DEFAULT 0,
    description TEXT
);

-- Bảng danh mục thiết bị (Equipments)
CREATE TABLE equipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    quantity INT NOT NULL DEFAULT 0,       -- Tổng số lượng trong kho
    available INT NOT NULL DEFAULT 0,      -- Số lượng thực tế có thể mượn
    unit VARCHAR(20) DEFAULT 'Cái',         -- Đơn vị (Bộ, chiếc, quyển,...)
    deposit_amount DECIMAL(15, 2) DEFAULT 0.00, -- Tiền đặt cọc mượn đồ nếu có
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng lịch hẹn tư vấn học thuật (Mentoring Sessions)
CREATE TABLE mentoring_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELED') DEFAULT 'PENDING',
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (lecturer_id) REFERENCES lecturers(id),
    UNIQUE (lecturer_id, session_date, start_time) -- Chống trùng lịch giảng viên trong cùng khung giờ
);

-- Bảng hồ sơ đánh giá kết quả (Academic Evaluations)
CREATE TABLE academic_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT UNIQUE NOT NULL,
    competency_evaluation TEXT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    FOREIGN KEY (session_id) REFERENCES mentoring_sessions(id) ON DELETE CASCADE
);

-- Bảng phiếu mượn thiết bị phòng Lab (Borrowing Records)
CREATE TABLE borrowing_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    status ENUM('PENDING_DISPATCH', 'DISPATCHED', 'RETURNED') DEFAULT 'PENDING_DISPATCH',
    FOREIGN KEY (session_id) REFERENCES mentoring_sessions(id)
);

-- Bảng chi tiết danh sách đồ mượn (Borrowing Details - Quan hệ nhiều-nhiều)
CREATE TABLE borrowing_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    FOREIGN KEY (record_id) REFERENCES borrowing_records(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipments(id)
);



-- 1. Chèn dữ liệu Loại phòng Lab mẫu
INSERT INTO lab_room_types (id, code, name, capacity, description) VALUES
    (1, 'LAB_LT',    'Phòng Lab Lập trình',   30, 'Phòng thực hành lập trình với máy tính cấu hình cao'),
    (2, 'LAB_MANG',  'Phòng Lab Mạng',        24, 'Phòng thực hành hệ thống mạng & thiết bị Cisco'),
    (3, 'LAB_DT',    'Phòng Lab Điện tử',     20, 'Phòng thực hành điện tử, vi mạch, IoT'),
    (4, 'LAB_AI',    'Phòng Lab AI/ML',       25, 'Phòng thực hành trí tuệ nhân tạo - GPU server'),
    (5, 'LAB_AN',    'Phòng Lab An toàn TT',  20, 'Phòng thực hành an toàn thông tin, pentest')
ON DUPLICATE KEY UPDATE name=VALUES(name), capacity=VALUES(capacity), description=VALUES(description);

-- 2. Chèn dữ liệu Khoa / Ngành mẫu
INSERT INTO departments (id, code, name, description, is_active) VALUES
    (1, 'CNTT', 'Công nghệ thông tin',     'Khoa Công nghệ thông tin & Truyền thông',  1),
    (2, 'ATTT', 'An toàn thông tin',       'Khoa An toàn thông tin',                   1),
    (3, 'KTPM', 'Kỹ thuật phần mềm',       'Khoa Kỹ thuật phần mềm',                   1),
    (4, 'KHMT', 'Khoa học máy tính',       'Khoa Khoa học máy tính & AI',              1)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description), is_active=VALUES(is_active);

-- 3. Chèn dữ liệu Danh mục thiết bị mẫu
INSERT INTO equipments (id, code, name, description, quantity, available, unit, deposit_amount, is_active, created_at, updated_at) VALUES
    (1, 'EQ-PC-01',      'Máy tính Dell OptiPlex 7090', 'PC i7-11700, 16GB RAM, 512GB SSD',     30, 28, 'Bộ',       500000.00,  1, NOW(), NOW()),
    (2, 'EQ-RT-01',      'Router Cisco 2911',           'Thiết bị thực hành mạng CCNA',         10,  8, 'Thiết bị', 200000.00,  1, NOW(), NOW()),
    (3, 'EQ-SW-01',      'Switch Cisco Catalyst 2960',  'Switch 24 cổng tốc độ 1Gbps',          15, 12, 'Thiết bị', 200000.00,  1, NOW(), NOW()),
    (4, 'EQ-AR-01',      'Arduino Uno R3',              'Board thực hành điện tử & IoT',        25, 22, 'Bộ',       50000.00,   1, NOW(), NOW()),
    (5, 'EQ-PI-01',      'Raspberry Pi 4 Model B',      'Mini PC 4GB RAM cho IoT & Edge AI',    20, 18, 'Bộ',       100000.00,  1, NOW(), NOW()),
    (6, 'EQ-OSC-01',     'Oscilloscope Rigol DS1054Z',  'Máy hiện sóng số 50MHz 4 kênh',         8,  7, 'Thiết bị', 1000000.00, 1, NOW(), NOW()),
    (7, 'EQ-BOOK-J01',   'Giáo trình Java cơ bản',      'Sách giáo trình Java SE 17',           50, 47, 'Quyển',    0.00,       1, NOW(), NOW()),
    (8, 'EQ-BOOK-DB01',  'Giáo trình Cơ sở dữ liệu',    'Sách giáo trình SQL & NoSQL',          50, 46, 'Quyển',    0.00,       1, NOW(), NOW()),
    (9, 'EQ-VR-01',      'Kính thực tế ảo Meta Quest 3','Thiết bị VR cho Lab AR/VR',             5,  4, 'Bộ',       2000000.00, 1, NOW(), NOW()),
    (10,'EQ-GPU-01',     'NVIDIA Jetson Nano',          'Edge AI development kit',              10,  9, 'Bộ',       500000.00,  1, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    name=VALUES(name), description=VALUES(description), quantity=VALUES(quantity), 
    available=VALUES(available), unit=VALUES(unit), deposit_amount=VALUES(deposit_amount), updated_at=NOW();

INSERT INTO users (id, username, password_hash, role, is_active) VALUES 
    (1, 'gv_nguyenvanan', '$2a$10$xyz...', 'LECTURER', 1),
    (2, 'gv_tranlebinh',  '$2a$10$xyz...', 'LECTURER', 1),
    (3, 'gv_phamthic',   '$2a$10$xyz...', 'LECTURER', 1);

INSERT INTO user_profiles (id, user_id, full_name, email, phone) VALUES
    (1, 1, 'Nguyễn Văn An', 'an.nv@smartacademic.edu.vn', '0901234567'),
    (2, 2, 'Trần Lê Bình',  'binh.tl@smartacademic.edu.vn','0918765432'),
    (3, 3, 'Phạm Thị Cúc',  'cuc.pt@smartacademic.edu.vn', '0983332211');

INSERT INTO lecturers (id, user_id, department_id, specialization, bio, session_fee) VALUES
    (1, 1, 1, 'Trí tuệ nhân tạo (AI)', 'Kinh nghiệm 10 năm nghiên cứu Machine Learning', 500000.00),
    (2, 2, 1, 'Lập trình Web & Microservices', 'Chuyên gia Java Spring Boot', 450000.00),
    (3, 3, 2, 'An toàn thông tin & Kỹ nghệ dịch ngược', 'Cựu chuyên gia Pentest', 600000.00);

SELECT u.id, u.username, u.role, p.full_name, p.email, p.phone, u.created_at
FROM users u
INNER JOIN user_profiles p ON u.id = p.user_id
WHERE u.is_active = 1
ORDER BY u.username ASC;

SELECT id, code, name, quantity, available, unit, deposit_amount
FROM equipments
WHERE is_active = 1
ORDER BY available DESC;

SELECT id, code, name, deposit_amount, unit
FROM equipments
ORDER BY deposit_amount ASC;
SELECT u.id AS user_id, u.username, p.full_name, l.specialization, l.session_fee, d.name AS department_name
FROM users u
INNER JOIN user_profiles p ON u.id = p.user_id
LEFT JOIN lecturers l ON u.id = l.user_id
LEFT JOIN departments d ON l.department_id = d.id
WHERE u.role = 'LECTURER' AND u.is_active = 1
ORDER BY p.full_name ASC;

SELECT u.id AS user_id, p.full_name, l.specialization, l.session_fee
FROM users u
INNER JOIN user_profiles p ON u.id = p.user_id
INNER JOIN lecturers l ON u.id = l.user_id
WHERE u.role = 'LECTURER' AND u.is_active = 1
ORDER BY l.session_fee DESC;

SELECT u.id AS user_id, p.full_name, l.specialization, d.name AS department_name
FROM users u
INNER JOIN user_profiles p ON u.id = p.user_id
INNER JOIN lecturers l ON u.id = l.user_id
INNER JOIN departments d ON l.department_id = d.id
WHERE u.role = 'LECTURER' AND l.department_id = 1 AND u.is_active = 1
ORDER BY l.specialization ASC;