-- V3: ERD 야도란 스키마 반영
-- 반영 사항:
--   1) rental_transactions는 V2 — 이 마이그레이션에서 손대지 않음
--   2) review_reports.handler_id는 NOT NULL로 확정
--   3) condition_gus.gu_code 단독 UNIQUE는 반영 X
--   4) gus/admin_dongs/gu_crime_stats/condition_gus는 실데이터 없음을 확인 후 DROP 후 재생성

-- =====================================================================
-- gus: 서로게이트 키(gu_id) 대신 gu_code(자연 키)를 PK로 사용
-- admin_dongs/gu_crime_stats/condition_gus가 gu_id를 참조하므로 함께 재생성
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `condition_gus`;
DROP TABLE IF EXISTS `gu_crime_stats`;
DROP TABLE IF EXISTS `admin_dongs`;
DROP TABLE IF EXISTS `gus`;

CREATE TABLE `gus` (
	`gu_code`	VARCHAR(20)	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`gu_code`),
	UNIQUE KEY `uq_gus_name` (`name`)
);

CREATE TABLE `admin_dongs` (
	`admin_dong_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`gu_code`	VARCHAR(20)	NOT NULL,
	`dong_code`	VARCHAR(20)	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`dong_population`	DECIMAL(8,2)	NULL,
	`dong_area`	DECIMAL(5,2)	NULL,
	`latitude`	DECIMAL(12,9)	NOT NULL,
	`longitude`	DECIMAL(12,9)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`admin_dong_id`),
	UNIQUE KEY `uq_admin_dongs_gu_dong_code` (`gu_code`, `dong_code`)
);

CREATE TABLE `gu_crime_stats` (
	`gu_crime_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`gu_code`	VARCHAR(20)	NOT NULL,
	`crime_rate`	DECIMAL(6,3)	NULL,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`gu_crime_id`)
);

CREATE TABLE `condition_gus` (
	`condition_id`	BIGINT	NOT NULL,
	`gu_code`	VARCHAR(20)	NOT NULL,
	PRIMARY KEY (`condition_id`, `gu_code`)
);

ALTER TABLE `admin_dongs` ADD CONSTRAINT `FK_gus_TO_admin_dongs_1` FOREIGN KEY (`gu_code`) REFERENCES `gus` (`gu_code`);
ALTER TABLE `gu_crime_stats` ADD CONSTRAINT `FK_gus_TO_gu_crime_stats_1` FOREIGN KEY (`gu_code`) REFERENCES `gus` (`gu_code`);
ALTER TABLE `condition_gus` ADD CONSTRAINT `FK_gus_TO_condition_gus_1` FOREIGN KEY (`gu_code`) REFERENCES `gus` (`gu_code`);
ALTER TABLE `condition_gus` ADD CONSTRAINT `FK_user_conditions_TO_condition_gus_1` FOREIGN KEY (`condition_id`) REFERENCES `user_conditions` (`condition_id`);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- places: 외부 데이터 연동을 위한 컬럼 추가, category enum 값 정리
-- =====================================================================

ALTER TABLE `places`
	ADD COLUMN `external_id` VARCHAR(200) NULL AFTER `place_id`,
	ADD COLUMN `data_source_id` BIGINT NOT NULL AFTER `longitude`,
	MODIFY COLUMN `category` ENUM('SPORTS','RIVER','TRAIL','PARK','CULTURE','BUS_STOP','SUBWAY_STATION','POLICE','STREET_LIGHT','SAFETY_BELL','CCTV','MART','DEPARTMENT_STORE','HOSPITAL','PHARMACY','BANK','CAFE','RESTAURANT','CONVENIENT_STORE','TOWN_OFFICE') NOT NULL;

ALTER TABLE `places` ADD CONSTRAINT `FK_data_sources_TO_places_1` FOREIGN KEY (`data_source_id`) REFERENCES `data_sources` (`data_source_id`);

-- =====================================================================
-- data_sources: description 필수화, 원본 데이터 URL 컬럼 추가
-- =====================================================================

ALTER TABLE `data_sources`
	MODIFY COLUMN `description` TEXT NOT NULL,
	ADD COLUMN `data_url` VARCHAR(500) NULL AFTER `description`;

-- =====================================================================
-- silence_metrics: 사용하지 않는 컬럼 제거
-- =====================================================================

ALTER TABLE `silence_metrics`
	DROP COLUMN `avg_noise`,
	DROP COLUMN `local_people_count`;

-- =====================================================================
-- data_collection_logs: data_source_id2 -> data_source_id 정상화
-- =====================================================================

ALTER TABLE `data_collection_logs` DROP FOREIGN KEY `FK_data_sources_TO_data_collection_logs_1`;
ALTER TABLE `data_collection_logs` CHANGE COLUMN `data_source_id2` `data_source_id` BIGINT NOT NULL;
ALTER TABLE `data_collection_logs` ADD CONSTRAINT `FK_data_sources_TO_data_collection_logs_1` FOREIGN KEY (`data_source_id`) REFERENCES `data_sources` (`data_source_id`);

-- =====================================================================
-- review_reports: handler_id NOT NULL로 확정
-- =====================================================================

ALTER TABLE `review_reports` MODIFY COLUMN `handler_id` BIGINT NOT NULL;

-- =====================================================================
-- users: birth_date 기본값을 유효한 리터럴로 수정
-- =====================================================================

ALTER TABLE `users` MODIFY COLUMN `birth_date` DATE NULL DEFAULT '20000101';
