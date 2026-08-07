-- V2: 국토부 전월세 실거래가 적재를 위한 임대차 거래 스키마 보완
-- 법정동-행정동 매핑 전까지 admin_dong_id는 비워둘 수 있게 하고,
-- API 응답의 구 코드와 법정동 이름을 별도 보존한다.

ALTER TABLE `rental_transactions`
    MODIFY COLUMN `admin_dong_id` BIGINT NULL,
    ADD COLUMN `gu_code` CHAR(5) NOT NULL AFTER `admin_dong_id`,
    ADD COLUMN `legal_dong_name` VARCHAR(100) NOT NULL AFTER `gu_code`,
    MODIFY COLUMN `house_type` VARCHAR(20) NOT NULL
        COMMENT '연립, 다세대, 연립다세대, 단독, 다가구, 오피스텔',
    ADD INDEX `idx_rental_transactions_gu_transaction_date` (`gu_code`, `transaction_date`);
