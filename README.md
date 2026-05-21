# Smart Academic & Lab Support Platform

Nền tảng hỗ trợ học thuật & quản lý thiết bị thực hành (Spring Boot 3 · Thymeleaf · MySQL · Tailwind).

## Yêu cầu
- JDK 21
- MySQL 8.x trở lên (cấu hình trong `src/main/resources/application.properties`)
- Gradle Wrapper sẵn trong repo (`./gradlew`)

## Chạy nhanh
```powershell
# 1. Tạo DB rỗng (Hibernate ddl-auto=update tự sinh bảng)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS smart_academic_db CHARACTER SET utf8mb4;"

# 2. Sửa user/password trong application.properties nếu cần

# 3. Chạy app
.\gradlew.bat bootRun
```
Mở http://localhost:8080.

## Tài khoản test (đã được DataSeeder hash BCrypt)
| Vai trò | Tài khoản | Mật khẩu |
|---------|-----------|----------|
| Admin | `admin` | `password123` |
| Giảng viên | `lecturer01`..`lecturer03` | `password123` |
| Sinh viên | `student01`..`student04` | `password123` |

## Tính năng chính
- Đặt lịch tư vấn với giảng viên + đánh giá học thuật
- Quản lý mượn/trả thiết bị (kèm cron job phát hiện quá hạn)
- Email thông báo bất đồng bộ (`@Async`)
- Dashboard thống kê + biểu đồ Chart.js
- **Thanh toán phí buổi tư vấn qua VNPay (sandbox + simulate mode)**

## Luồng thanh toán
1. Sinh viên đặt lịch với giảng viên có `session_fee > 0` → lịch ở trạng thái **CHỜ THANH TOÁN**.
2. Sinh viên bấm "Thanh toán" → app tạo `Payment(PENDING)` và redirect:
   - `app.payment.simulate-mode=true` (mặc định): mở trang giả lập trong app, có 2 nút "Xác nhận" / "Hủy".
   - `app.payment.simulate-mode=false`: dựng URL kèm chữ ký HMAC-SHA512, redirect sang VNPay sandbox (cần `tmn-code` + `hash-secret` thật).
3. Sau khi thanh toán, gateway redirect về `/payment/vnpay-return?...&vnp_SecureHash=...`. Controller verify chữ ký + số tiền rồi:
   - `vnp_ResponseCode=00` → Payment SUCCESS, MentoringSession chuyển sang **CHỜ XÁC NHẬN**, gửi email.
   - Khác → Payment FAILED, lịch giữ nguyên CHỜ THANH TOÁN (sinh viên có thể bấm thanh toán lại).
4. Trang `/payment/history` liệt kê toàn bộ giao dịch của sinh viên.

## Đổi sang VNPay sandbox thật
1. Đăng ký merchant TEST tại **https://sandbox.vnpayment.vn/devreg**
   (trang gốc `sandbox.vnpayment.vn` để trống là bình thường — phải vào đúng đường dẫn `/devreg`).
   - Điền thông tin + URL website/ứng dụng + captcha → nhận email kích hoạt.
   - Email thứ 2 sẽ chứa **TmnCode** (mã website) và **HashSecret** (chuỗi bí mật).
2. Sửa `application.properties`:
   ```properties
   app.payment.simulate-mode=false
   app.payment.vnpay.tmn-code=<TmnCode VNPay gửi>
   app.payment.vnpay.hash-secret=<HashSecret VNPay gửi>
   ```
3. Nếu deploy ngoài localhost, đổi `app.payment.vnpay.return-url` thành domain thật.
4. Tham khảo tài liệu chính thức: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html

### Thẻ test VNPay sandbox (để demo thanh toán thành công)
| Trường | Giá trị |
|--------|---------|
| Ngân hàng | NCB |
| Chủ thẻ | NGUYEN VAN A |
| Số thẻ | `9704198526191432198` |
| Ngày phát hành | `07/15` |
| OTP | `123456` |

> Trong khi chờ kích hoạt merchant, cứ giữ `app.payment.simulate-mode=true` để demo bằng trang giả lập tích hợp sẵn trong app — luồng nghiệp vụ giống y hệt VNPay thật (cùng entity `Payment`, cùng việc cập nhật trạng thái session).

## Cấu trúc thư mục
```
src/main/java/com/smartacademic
├── config/        # AppConfig (Security), VNPayConfig, AsyncConfig, DataSeeder
├── controller/    # Auth, Student, Lecturer, Admin, Payment, Home, Profile
├── entity/        # User, MentoringSession, Equipment, BorrowingRecord, Payment, ...
├── enums/         # Role, SessionStatus, BorrowingStatus, PaymentStatus, PaymentType
├── repository/    # Interface + Impl (Hibernate Session qua HibernateSessionProvider)
├── service/       # Business logic (MentoringSession, Borrowing, Payment, Email, Statistics)
├── scheduler/     # OverdueBorrowingScheduler (@Scheduled)
└── util/          # VNPayUtils (HMAC-SHA512, verify signature)

src/main/resources/templates
├── auth/          # login.html, register.html
├── student/       # dashboard, booking, history, profile
├── lecturer/      # dashboard, evaluate, profile
├── admin/         # dashboard, statistics, equipment, borrowing, ...
├── payment/       # simulate, result, history
└── fragments/     # layout.html (sidebar + topbar dùng chung)
```
