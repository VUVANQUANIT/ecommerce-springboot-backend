# 📚 HƯỚNG DẪN CHI TIẾT CHẠY DỰ ÁN E-COMMERCE

## 🎯 MỤC LỤC
1. [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
2. [Cài đặt Docker](#cài-đặt-docker)
3. [Cấu trúc dự án](#cấu-trúc-dự-an)
4. [Hướng dẫn chạy với Docker](#hướng-dẫn-chạy-với-docker)
5. [Hướng dẫn chạy không dùng Docker](#hướng-dẫn-chạy-không-dùng-docker)
6. [Truy cập Swagger UI](#truy-cập-swagger-ui)
7. [Test API](#test-api)

---

## 📋 YÊU CẦU HỆ THỐNG

### Phần mềm cần cài đặt:
1. **Java 21** - Để compile và chạy ứng dụng
2. **Maven 3.9+** - Để build dự án
3. **Docker Desktop** - Để chạy database và Redis
4. **Postman hoặc trình duyệt** - Để test API

### Kiểm tra cài đặt:
```bash
# Kiểm tra Java
java -version
# Kết quả mong đợi: openjdk version "21"...

# Kiểm tra Maven
mvn -version
# Kết quả mong đợi: Apache Maven 3.9...

# Kiểm tra Docker
docker --version
# Kết quả mong đợi: Docker version 24...
```

---

## 🐳 CÀI ĐẶT DOCKER

### Bước 1: Tải Docker Desktop
- Windows: https://www.docker.com/products/docker-desktop/
- Cài đặt và khởi động lại máy

### Bước 2: Kiểm tra Docker đang chạy
```bash
docker ps
# Nếu không có lỗi là OK
```

---

## 📁 CẤU TRÚC DỰ ÁN

```
demo/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/demo/
│       │       ├── auth/          # Authentication & Authorization
│       │       ├── config/         # Cấu hình (Security, Redis, Swagger)
│       │       ├── security/       # JWT Security
│       │       └── user/           # User Management
│       └── resources/
│           └── application.yml     # Cấu hình database, Redis, JWT
├── pom.xml                        # Maven dependencies
├── Dockerfile                      # Cấu hình build Docker image
└── docker-compose.yml             # Cấu hình các services (App, Postgres, Redis)
```

---

## 🚀 HƯỚNG DẪN CHẠY VỚI DOCKER

### CÁCH 1: Chạy tất cả với Docker Compose (KHUYẾN NGHỊ)

#### Bước 1: Build và chạy tất cả services
```bash
# Di chuyển vào thư mục dự án
cd C:\Users\quanc\Documents\Spring_Boot\EcommerceBackendApplication\demo

# Build và chạy tất cả (Postgres, Redis, App)
docker-compose up --build
```

**Giải thích:**
- `docker-compose up`: Khởi động tất cả services trong file docker-compose.yml
- `--build`: Build lại Docker image trước khi chạy
- Lần đầu chạy sẽ mất 5-10 phút để download images

#### Bước 2: Kiểm tra services đang chạy
```bash
# Mở terminal mới và chạy
docker ps
```

**Kết quả mong đợi:**
```
CONTAINER ID   IMAGE                    STATUS
xxx            demo-app                 Up 2 minutes
xxx            postgres:16              Up 2 minutes  
xxx            redis:7                  Up 2 minutes
```

#### Bước 3: Xem logs
```bash
# Xem logs của tất cả services
docker-compose logs -f

# Hoặc xem logs của từng service
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f redis
```

#### Bước 4: Dừng services
```bash
# Dừng tất cả services
docker-compose down

# Dừng và xóa volumes (xóa database)
docker-compose down -v
```

---

### CÁCH 2: Chạy chỉ Database và Redis với Docker, App chạy local

#### Bước 1: Chạy Postgres và Redis
```bash
# Chỉ chạy Postgres và Redis
docker-compose up postgres redis -d
```

**Giải thích:**
- `-d`: Chạy ở background (detached mode)
- Chỉ chạy 2 services: postgres và redis

#### Bước 2: Sửa application.yml để kết nối localhost
Mở file `src/main/resources/application.yml` và sửa:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_db  # Đổi từ postgres thành localhost
  data:
    redis:
      host: localhost  # Đổi từ redis thành localhost
```

#### Bước 3: Build và chạy ứng dụng
```bash
# Build dự án
mvn clean package -DskipTests

# Chạy ứng dụng
mvn spring-boot:run
```

---

## 🌐 TRUY CẬP SWAGGER UI

### Sau khi ứng dụng chạy thành công:

1. **Mở trình duyệt** và truy cập:
   ```
   http://localhost:8080/swagger-ui.html
   ```
   hoặc
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

2. **Bạn sẽ thấy giao diện Swagger** với tất cả các API endpoints

3. **Các API chính:**
   - `/api/v1/auth/register` - Đăng ký user mới
   - `/api/v1/auth/login` - Đăng nhập
   - `/api/v1/auth/refresh` - Refresh token
   - `/api/v1/auth/logout` - Đăng xuất
   - `/api/v1/user/profile` - Xem profile (cần đăng nhập)
   - `/api/v1/admin/users` - Quản lý users (chỉ ADMIN)

---

## 🧪 TEST API

### Bước 1: Đăng ký user mới

**Request:**
```http
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "fullName": "Nguyen Van A",
  "email": "user@example.com",
  "password": "123456"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "fullname": "Nguyen Van A",
  "role": "USER"
}
```

### Bước 2: Đăng nhập

**Request:**
```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "123456"
}
```

### Bước 3: Sử dụng Access Token

**Request:**
```http
GET http://localhost:8080/api/v1/user/profile
Authorization: Bearer <accessToken>
```

**Giải thích:**
- Copy `accessToken` từ response đăng nhập
- Thêm header `Authorization: Bearer <token>` vào request

---

## 🔧 XỬ LÝ LỖI THƯỜNG GẶP

### Lỗi 1: Port 8080 đã được sử dụng
```bash
# Tìm process đang dùng port 8080
netstat -ano | findstr :8080

# Kill process (thay PID bằng process ID)
taskkill /PID <PID> /F
```

### Lỗi 2: Docker không chạy
```bash
# Kiểm tra Docker Desktop đang chạy chưa
# Nếu chưa, mở Docker Desktop và đợi nó khởi động xong
```

### Lỗi 3: Database connection failed
```bash
# Kiểm tra Postgres container đang chạy
docker ps | grep postgres

# Xem logs để biết lỗi
docker-compose logs postgres
```

### Lỗi 4: Redis connection failed
```bash
# Kiểm tra Redis container
docker ps | grep redis

# Test kết nối Redis
docker exec -it <redis-container-id> redis-cli ping
# Kết quả: PONG
```

---

## 📝 CÁC LỆNH DOCKER HỮU ÍCH

```bash
# Xem tất cả containers (đang chạy và đã dừng)
docker ps -a

# Xem logs của container
docker logs <container-id>

# Vào trong container
docker exec -it <container-id> /bin/bash

# Xem images
docker images

# Xóa container
docker rm <container-id>

# Xóa image
docker rmi <image-id>

# Dọn dẹp (xóa containers, images không dùng)
docker system prune -a
```

---

## ✅ CHECKLIST TRƯỚC KHI CHẠY

- [ ] Java 21 đã cài đặt
- [ ] Maven đã cài đặt
- [ ] Docker Desktop đang chạy
- [ ] Port 8080, 5432, 6379 chưa bị chiếm dụng
- [ ] Đã chạy `mvn clean compile` thành công
- [ ] File `application.yml` đã cấu hình đúng

---

## 🎓 GIẢI THÍCH CÁC THÀNH PHẦN

### Docker Compose
- **postgres**: Database PostgreSQL lưu trữ dữ liệu user
- **redis**: Cache lưu refresh tokens
- **app**: Ứng dụng Spring Boot của bạn

### Application.yml
- **datasource**: Cấu hình kết nối PostgreSQL
- **redis**: Cấu hình kết nối Redis
- **jwt**: Cấu hình JWT tokens (secret, expiration)

### Security Flow
1. User đăng ký/đăng nhập → Nhận access token và refresh token
2. Access token dùng để gọi API (hết hạn sau 24h)
3. Refresh token dùng để lấy access token mới (hết hạn sau 7 ngày)
4. Refresh token được lưu trong Redis

---

Chúc bạn thành công! 🎉

