DROP DATABASE smart_academic_db;
CREATE DATABASE IF NOT EXISTS smart_academic_db;
USE smart_academic_db;
DROP TABLE IF EXISTS lecturers;
DROP TABLE IF EXISTS departments;
ALTER TABLE lecturers DROP FOREIGN KEY lecturers_ibfk_2;

-- 2. Đồng bộ cả 2 cột về cùng một kiểu dữ liệu BIGINT
ALTER TABLE departments MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE lecturers MODIFY COLUMN department_id BIGINT NOT NULL;

-- 3. Tạo lại ràng buộc khóa ngoại
ALTER TABLE lecturers 
ADD CONSTRAINT lecturers_ibfk_2 
FOREIGN KEY (department_id) REFERENCES departments(id);
-- 1. Bảng users (Tài khoản)
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL, -- Phải băm mật khẩu
                       role ENUM('STUDENT', 'LECTURER', 'ADMIN') NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng user_profiles (Hồ sơ người dùng)
CREATE TABLE user_profiles (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT UNIQUE NOT NULL,
                               full_name VARCHAR(100) NOT NULL,
                               email VARCHAR(100) UNIQUE,
                               phone VARCHAR(15),
                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Bảng departments (Khoa/Ngành - Dữ liệu nền)
CREATE TABLE departments (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             department_name VARCHAR(100) NOT NULL UNIQUE
);

-- 4. Bảng lecturers (Thông tin Giảng viên)
CREATE TABLE lecturers (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT UNIQUE NOT NULL,
                           department_id INT NOT NULL,
                           specialization VARCHAR(255),
                           FOREIGN KEY (user_id) REFERENCES users(id),
                           FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- 5. Bảng equipments (Danh mục Thiết bị)
CREATE TABLE equipments (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            description TEXT,
                            stock_quantity INT NOT NULL DEFAULT 0,
                            status ENUM('AVAILABLE', 'MAINTENANCE') DEFAULT 'AVAILABLE'
);

-- 6. Bảng mentoring_sessions (Lịch hẹn tư vấn)
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
                                    UNIQUE (lecturer_id, session_date, start_time) -- Chống trùng lịch giảng viên
);

-- 7. Bảng academic_evaluations (Hồ sơ Đánh giá)
CREATE TABLE academic_evaluations (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      session_id BIGINT UNIQUE NOT NULL,
                                      competency_evaluation TEXT NOT NULL,
                                      rating INT CHECK (rating >= 1 AND rating <= 5),
                                      FOREIGN KEY (session_id) REFERENCES mentoring_sessions(id)
);

-- 8. Bảng borrowing_records (Phiếu mượn thiết bị)
CREATE TABLE borrowing_records (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   session_id BIGINT NOT NULL,
                                   status ENUM('PENDING_DISPATCH', 'DISPATCHED', 'RETURNED') DEFAULT 'PENDING_DISPATCH',
                                   FOREIGN KEY (session_id) REFERENCES mentoring_sessions(id)
);

-- 9. Bảng borrowing_details (Chi tiết phiếu mượn N-N)
CREATE TABLE borrowing_details (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   record_id BIGINT NOT NULL,
                                   equipment_id BIGINT NOT NULL,
                                   quantity INT NOT NULL CHECK (quantity > 0),
                                   FOREIGN KEY (record_id) REFERENCES borrowing_records(id),
                                   FOREIGN KEY (equipment_id) REFERENCES equipments(id)
);