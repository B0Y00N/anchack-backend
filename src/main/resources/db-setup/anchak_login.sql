-- 1. 데이터베이스 생성 (이미 있으면 생략)
CREATE DATABASE anchak_login
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 사용자 생성
CREATE USER 'admin'@'localhost'
    IDENTIFIED BY '1234';

-- 3. 권한 부여
GRANT ALL PRIVILEGES
    ON anchak_login.*
    TO 'admin'@'localhost';

-- 4. 권한 적용
FLUSH PRIVILEGES;