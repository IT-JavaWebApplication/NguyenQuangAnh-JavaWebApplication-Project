-- ============================================================
-- SEED DATA - Smart Academic Lab Support Platform
-- USERS/LECTURERS/SESSIONS/EVAL/BORROWING được seed bằng DataSeeder (CommandLineRunner) để mật khẩu được hash đúng.
--  seed các bảng nền: departments, lab_room_types, equipments.

-- LAB ROOM TYPES (seed)
INSERT IGNORE INTO lab_room_types (id, code, name, capacity, description) VALUES
    (1, 'LAB_LT',    'Phòng Lab Lập trình',   30, 'Phòng thực hành lập trình với máy tính cấu hình cao'),
    (2, 'LAB_MANG',  'Phòng Lab Mạng',        24, 'Phòng thực hành hệ thống mạng & thiết bị Cisco'),
    (3, 'LAB_DT',    'Phòng Lab Điện tử',     20, 'Phòng thực hành điện tử, vi mạch, IoT'),
    (4, 'LAB_AI',    'Phòng Lab AI/ML',       25, 'Phòng thực hành trí tuệ nhân tạo - GPU server'),
    (5, 'LAB_AN',    'Phòng Lab An toàn TT',  20, 'Phòng thực hành an toàn thông tin, pentest');

-- DEPARTMENTS (seed)
INSERT IGNORE INTO departments (id, code, name, description, is_active) VALUES
    (1, 'CNTT', 'Công nghệ thông tin',     'Khoa Công nghệ thông tin & Truyền thông',  1),
    (2, 'ATTT', 'An toàn thông tin',       'Khoa An toàn thông tin',                   1),
    (3, 'KTPM', 'Kỹ thuật phần mềm',       'Khoa Kỹ thuật phần mềm',                   1),
    (4, 'KHMT', 'Khoa học máy tính',       'Khoa Khoa học máy tính & AI',              1);

-- EQUIPMENTS (seed)
INSERT IGNORE INTO equipments (id, code, name, description, quantity, available, unit, deposit_amount, is_active, created_at, updated_at) VALUES
    (1, 'EQ-PC-01',      'Máy tính Dell OptiPlex 7090', 'PC i7-11700, 16GB RAM, 512GB SSD',     30, 28, 'Bộ',       500000, 1, NOW(), NOW()),
    (2, 'EQ-RT-01',      'Router Cisco 2911',           'Thiết bị thực hành mạng CCNA',         10,  8, 'Thiết bị', 200000, 1, NOW(), NOW()),
    (3, 'EQ-SW-01',      'Switch Cisco Catalyst 2960',  'Switch 24 cổng tốc độ 1Gbps',          15, 12, 'Thiết bị', 200000, 1, NOW(), NOW()),
    (4, 'EQ-AR-01',      'Arduino Uno R3',              'Board thực hành điện tử & IoT',        25, 22, 'Bộ',       50000,  1, NOW(), NOW()),
    (5, 'EQ-PI-01',      'Raspberry Pi 4 Model B',      'Mini PC 4GB RAM cho IoT & Edge AI',    20, 18, 'Bộ',       100000, 1, NOW(), NOW()),
    (6, 'EQ-OSC-01',     'Oscilloscope Rigol DS1054Z',  'Máy hiện sóng số 50MHz 4 kênh',         8,  7, 'Thiết bị', 1000000,1, NOW(), NOW()),
    (7, 'EQ-BOOK-J01',   'Giáo trình Java cơ bản',      'Sách giáo trình Java SE 17',           50, 47, 'Quyển',    0,      1, NOW(), NOW()),
    (8, 'EQ-BOOK-DB01',  'Giáo trình Cơ sở dữ liệu',    'Sách giáo trình SQL & NoSQL',          50, 46, 'Quyển',    0,      1, NOW(), NOW()),
    (9, 'EQ-VR-01',      'Kính thực tế ảo Meta Quest 3','Thiết bị VR cho Lab AR/VR',             5,  4, 'Bộ',       2000000,1, NOW(), NOW()),
    (10,'EQ-GPU-01',     'NVIDIA Jetson Nano',          'Edge AI development kit',              10,  9, 'Bộ',       500000, 1, NOW(), NOW());
