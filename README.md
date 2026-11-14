# 🎓 StudentHub — Hệ thống Quản lý Sinh viên & Đào tạo (Console)

Ứng dụng Console viết bằng **Java 21 + JDBC + MariaDB**, thiết kế phân tầng chuẩn (`Model - DTO - DAO - Service - UI`), hỗ trợ phân quyền 3 Role (`ADMIN`, `TEACHER`, `STUDENT`), băm mật khẩu **BCrypt**, khóa tài khoản tự động sau 5 lần đăng nhập sai, **Transaction**, **Import/Export CSV (UTF-8 BOM)** và giao diện **ANSI Color**.

---

## 🛠️ Tech Stack
* **Java:** JDK 21
* **Build Tool:** Maven
* **Database:** MariaDB (`mariadb-java-client:3.3.3`)
* **Security:** BCrypt (`jbcrypt:0.4`)
* **Boilerplate Reduction:** Lombok (`lombok:1.18.34`)
* **Logging:** `java.util.logging` (ghi ra `logs/app.log`)

---

## 🚀 Hướng dẫn Cài đặt & Chạy

1. **Khởi động MariaDB Server** (cổng mặc định `3306`).
2. Kiểm tra hoặc chỉnh sửa tài khoản MariaDB trong file [`src/main/resources/db.properties`](src/main/resources/db.properties):
   ```properties
   db.url=jdbc:mariadb://localhost:3306/studenthub?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8
   db.username=root
   db.password=root
   ```
3. **Chạy chương trình:**
   * Mở project trong IntelliJ IDEA và chạy [`src/main/java/com/studenthub/Main.java`](src/main/java/com/studenthub/Main.java).
   * Khi chạy lần đầu, `DatabaseInitializer` sẽ **tự động tạo Database `studenthub`, tạo đủ 7 bảng (`schema.sql`) và seed dữ liệu mẫu**.

---

## 🔑 Tài khoản Mẫu (Seed tự động)

| Role | Username | Mật khẩu | Mô tả |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `Admin@123` | Quản trị toàn hệ thống |
| **TEACHER** | `gv001` | `123456` | TS. Nguyễn Văn An (Dạy Java & CTDL) |
| **TEACHER** | `gv002` | `123456` | ThS. Trần Thị Bình (Dạy CSDL) |
| **STUDENT** | `SV001` | `123456` | SV Lê Hoàng Nam (Lớp `DHKTPM18A`) |
| **STUDENT** | `SV002` | `123456` | SV Phạm Ngọc Mai (Lớp `DHKTPM18A`) |
| **STUDENT** | `SV003` | `123456` | SV Đỗ Minh Tuấn (Lớp `DHHTTT18B`) |

---

## 📂 File mẫu thử nghiệm Import CSV
Bạn có thể dùng file [`sample_students_import.csv`](sample_students_import.csv) ở thư mục gốc của project để test chức năng **Import sinh viên từ CSV** trong menu của `ADMIN`.
