-- V2: 안착 서비스 초기 ERD 스키마
-- draft.sql(ERD 산출물) 기반, 다음 5가지를 CREATE TABLE 정의 자체에 반영함
--   1) recommendations.status DEFAULT 'VALID' (따옴표 누락 수정)
--   2) review_reports.handler_id NULL 허용
--   3) UNIQUE 제약 추가 (gus, admin_dongs, data_sources, review_categories, users)
--   4) 서지게이트 BIGINT 단일 PK 테이블에 AUTO_INCREMENT 추가
--   5) admin_dong_id/data_date, reviews, recommendations 인덱스 추가
-- FK는 전체 34개로 확장 (admin_dongs/users/user_conditions/recommendations/reviews 등 모든 참조 컬럼 커버)
-- 그 외: birth_date DEFAULT YYYYMMDD(유효하지 않은 MySQL 문법) 제거

DROP TABLE IF EXISTS `transit_metrics`;

CREATE TABLE `transit_metrics` (
	`transit_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`transit_score`	DECIMAL(6,2)	NOT NULL	DEFAULT 0,
	`subway_station_count`	INT	NOT NULL	DEFAULT 0,
	`bus_stop_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`transit_id`),
	INDEX `idx_transit_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `rental_transactions`;

CREATE TABLE `rental_transactions` (
	`transaction_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`transaction_date`	DATE	NOT NULL,
	`house_type`	ENUM('오피스텔', '빌라','단독 다가구', '아파트', '원룸')	NOT NULL,
	`area`	DECIMAL(5,2)	NOT NULL,
	`deposit`	BIGINT	NOT NULL	DEFAULT 0,
	`rent`	BIGINT	NOT NULL	DEFAULT 0,
	`maintenance_fee`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`transaction_id`)
);

DROP TABLE IF EXISTS `review_categories`;

CREATE TABLE `review_categories` (
	`review_category_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`code`	ENUM('NOISE', 'CLEANLINESS', 'SAFETY', 'ATMOSPHERE', 'TRANSIT')	NOT NULL,
	PRIMARY KEY (`review_category_id`),
	UNIQUE KEY `uq_review_categories_code` (`code`)
);

DROP TABLE IF EXISTS `gus`;

CREATE TABLE `gus` (
	`gu_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`name`	VARCHAR(100)	NOT NULL,
	`code`	VARCHAR(20)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`gu_id`),
	UNIQUE KEY `uq_gus_name` (`name`),
	UNIQUE KEY `uq_gus_code` (`code`)
);

DROP TABLE IF EXISTS `data_sources`;

CREATE TABLE `data_sources` (
	`data_source_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`name`	VARCHAR(200)	NOT NULL,
	`type`	VARCHAR(100)	NOT NULL,
	`description`	TEXT	NULL,
	`is_active`	BOOLEAN	NOT NULL	DEFAULT TRUE,
	`data_date`	DATE	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`data_source_id`),
	UNIQUE KEY `uq_data_sources_name` (`name`)
);

DROP TABLE IF EXISTS `recommendation_scores`;

CREATE TABLE `recommendation_scores` (
	`rec_score_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`recommendation_id`	BIGINT	NOT NULL,
	`category`	ENUM('TRANSIT', 'SAFETY', 'SPORTS', 'FOOD', 'SHOPPING', 'CULTURE', 'NATURE', 'SILENCE')	NOT NULL,
	`raw_score`	DECIMAL(5,2)	NOT NULL,
	`weight`	DECIMAL(5,4)	NOT NULL,
	`weighted_score`	DECIMAL(6,2)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`rec_score_id`)
);

DROP TABLE IF EXISTS `silence_metrics`;

CREATE TABLE `silence_metrics` (
	`silence_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`silence_score`	DECIMAL(6,2)	NULL,
	`avg_noise`	INT	NULL,
	`local_people_count`	INT	NULL,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`silence_id`)
);

DROP TABLE IF EXISTS `safety_metrics`;

CREATE TABLE `safety_metrics` (
	`safety_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`safety_score`	DECIMAL(6,2)	NOT NULL	DEFAULT 0,
	`cctv_count`	INT	NOT NULL	DEFAULT 0,
	`street_light_count`	INT	NOT NULL	DEFAULT 0,
	`police_office_count`	INT	NOT NULL	DEFAULT 0,
	`safety_bell_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`safety_id`),
	INDEX `idx_safety_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `sports_metrics`;

CREATE TABLE `sports_metrics` (
	`sports_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`sports_score`	DECIMAL(6,2)	NOT NULL	DEFAULT 0,
	`sports_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`sports_id`),
	INDEX `idx_sports_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `gu_crime_stats`;

CREATE TABLE `gu_crime_stats` (
	`gu_crime_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`gu_id`	BIGINT	NOT NULL,
	`crime_rate`	DECIMAL(6, 3)	NULL,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`gu_crime_id`)
);

DROP TABLE IF EXISTS `nature_metrics`;

CREATE TABLE `nature_metrics` (
	`nature_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`nature_score`	DECIMAL(6,2)	NOT NULL	DEFAULT 0,
	`nature_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`nature_id`),
	INDEX `idx_nature_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `review_reports`;

CREATE TABLE `review_reports` (
	`report_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`review_id`	BIGINT	NOT NULL,
	`reporter_id`	BIGINT	NOT NULL,
	`handler_id`	BIGINT	NULL,
	`reason`	VARCHAR(255)	NOT NULL,
	`status`	ENUM('PENDING', 'ACCEPTED', 'REJECTED')	NOT NULL	DEFAULT 'PENDING',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`handled_at`	DATETIME	NULL,
	PRIMARY KEY (`report_id`)
);

DROP TABLE IF EXISTS `property_metrics`;

CREATE TABLE `property_metrics` (
	`property_metric_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`rental_type`	ENUM('전세','월세')	NOT NULL,
	`house_type`	ENUM('오피스텔', '빌라','단독다가구', '아파트', '원룸')	NOT NULL,
	`min_area`	DECIMAL(5,2)	NOT NULL	DEFAULT 0,
	`max_area`	DECIMAL(5,2)	NOT NULL	DEFAULT 0,
	`avg_deposit`	BIGINT	NOT NULL	DEFAULT 0,
	`avg_rent`	BIGINT	NOT NULL	DEFAULT 0,
	`transaction_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`property_metric_id`),
	INDEX `idx_property_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `preferred_house_types`;

CREATE TABLE `preferred_house_types` (
	`preferred_house_type_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`condition_id`	BIGINT	NOT NULL,
	`house_type`	ENUM('오피스텔', '빌라','단독다가구', '아파트', '원룸')	NOT NULL,
	PRIMARY KEY (`preferred_house_type_id`)
);

DROP TABLE IF EXISTS `culture_metrics`;

CREATE TABLE `culture_metrics` (
	`culture_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`culture_score`	DECIMAL(6,2)	NOT NULL	DEFAULT 0,
	`culture_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`culture_id`),
	INDEX `idx_culture_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
	`user_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`email`	VARCHAR(255)	NULL,
	`password`	VARCHAR(255)	NULL,
	`name`	VARCHAR(100)	NULL,
	`nickname`	VARCHAR(100)	NULL,
	`birth_date`	DATE	NULL,
	`gender`	ENUM('M','F','N')	NOT NULL	DEFAULT 'N',
	`profile_image_url`	VARCHAR(500)	NULL,
	`provider`	ENUM('LOCAL', 'KAKAO')	NOT NULL	DEFAULT 'LOCAL'	COMMENT 'part of UNIQUE(provider, provider_id)',
	`provider_id`	VARCHAR(100)	NULL	COMMENT 'part of UNIQUE(provider, provider_id)',
	`status`	ENUM('ACTIVE', 'INACTIVE', 'WITHDRAWN')	NOT NULL	DEFAULT 'ACTIVE',
	`role`	ENUM('USER', 'ADMIN')	NOT NULL	DEFAULT 'USER',
	`last_login_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`deleted_at`	DATETIME	NULL,
	PRIMARY KEY (`user_id`),
	UNIQUE KEY `uq_users_email` (`email`),
	UNIQUE KEY `uq_users_nickname` (`nickname`),
	UNIQUE KEY `uq_users_provider_provider_id` (`provider`, `provider_id`)
);

DROP TABLE IF EXISTS `healthcare_metrics`;

CREATE TABLE `healthcare_metrics` (
	`healthcare_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`healthcare_score`	DECIMAL(6, 2)	NULL,
	`hospital_count`	INT	NULL,
	`pharmacy_count`	INT	NULL,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`healthcare_id`)
);

DROP TABLE IF EXISTS `food_metrics`;

CREATE TABLE `food_metrics` (
	`food_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`food_score`	DECIMAL(6, 2)	NULL,
	`restaurant_count`	INT	NULL,
	`cafe_count`	INT	NULL,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`food_id`)
);

DROP TABLE IF EXISTS `places`;

CREATE TABLE `places` (
	`place_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`category`	ENUM('SPORTS','RIVER','TRAIL','PARK','CULTURE','BUS_STATION','SUBWAY_STATION','POLICE','STREETLIGHT','SAFETY_BELL','CCTV','MART', 'DEPARTMENT_STORE', 'HOSPITAL','PHARMACY','BANK','CAFE','RESTAURANT','CONVENIENT_STORE','TOWN_OFFICE')	NOT NULL,
	`name`	VARCHAR(200)	NOT NULL,
	`latitude`	DECIMAL(9,6)	NOT NULL,
	`longitude`	DECIMAL(9,6)	NOT NULL,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`place_id`)
);

DROP TABLE IF EXISTS `favorite_dongs`;

CREATE TABLE `favorite_dongs` (
	`user_id`	BIGINT	NOT NULL,
	`admin_dong_id`	BIGINT	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`user_id`, `admin_dong_id`)
);

DROP TABLE IF EXISTS `data_collection_logs`;

CREATE TABLE `data_collection_logs` (
	`log_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`data_source_id2`	BIGINT	NOT NULL,
	`status`	ENUM('SUCCESS', 'FAILED')	NOT NULL,
	`record_count`	INT	NOT NULL	DEFAULT 0,
	`message`	VARCHAR(500)	NOT NULL,
	`collected_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`log_id`)
);

DROP TABLE IF EXISTS `admin_dongs`;

CREATE TABLE `admin_dongs` (
	`admin_dong_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`gu_id`	BIGINT	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`code`	VARCHAR(20)	NOT NULL,
	`latitude`	DECIMAL(12,9)	NOT NULL,
	`longitude`	DECIMAL(12,9)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`admin_dong_id`),
	UNIQUE KEY `uq_admin_dongs_name` (`name`),
	UNIQUE KEY `uq_admin_dongs_code` (`code`)
);

DROP TABLE IF EXISTS `condition_gus`;

CREATE TABLE `condition_gus` (
	`gu_id`	BIGINT	NOT NULL,
	`condition_id`	BIGINT	NOT NULL,
	PRIMARY KEY (`gu_id`, `condition_id`)
);

DROP TABLE IF EXISTS `condition_weights`;

CREATE TABLE `condition_weights` (
	`category`	ENUM('TRANSIT', 'SAFETY', 'SPORTS', 'FOOD', 'SHOPPING', 'CULTURE', 'NATURE','SILENT')	NOT NULL,
	`condition_id2`	BIGINT	NOT NULL,
	`importance`	DECIMAL(5,2)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`category`, `condition_id2`)
);

DROP TABLE IF EXISTS `review_scores`;

CREATE TABLE `review_scores` (
	`review_id`	BIGINT	NOT NULL,
	`review_category_id`	BIGINT	NOT NULL,
	`score`	TINYINT	NOT NULL	DEFAULT 0,
	PRIMARY KEY (`review_id`, `review_category_id`)
);

DROP TABLE IF EXISTS `condition_essentials`;

CREATE TABLE `condition_essentials` (
	`category`	ENUM('편의점', '헬스장', '병원', '공원', '대형마트')	NOT NULL,
	`condition_id2`	BIGINT	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`category`, `condition_id2`)
);

DROP TABLE IF EXISTS `life_convenience_metrics`;

CREATE TABLE `life_convenience_metrics` (
	`convenience_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`convenience_score`	DECIMAL(6,2)	NULL,
	`mart_count`	INT	NOT NULL	DEFAULT 0,
	`bank_count`	INT	NOT NULL	DEFAULT 0,
	`department_store_count`	INT	NOT NULL	DEFAULT 0,
	`data_date`	DATE	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`convenience_id`),
	INDEX `idx_life_convenience_metrics_admin_dong_data_date` (`admin_dong_id`, `data_date` DESC)
);

DROP TABLE IF EXISTS `user_conditions`;

CREATE TABLE `user_conditions` (
	`condition_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`rental_type`	ENUM('전세','월세')	NOT NULL,
	`dest_address`	TEXT	NULL,
	`commute_type`	ENUM('자가용', '대중교통')	NULL,
	`max_commute_time`	INT	NULL,
	`max_transfer_count`	INT	NULL,
	`min_area`	DECIMAL(5,2)	NULL,
	`max_deposit`	BIGINT	NULL,
	`max_rent`	BIGINT	NULL,
	`max_maintenance_fee`	INT	NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`condition_id`)
);

DROP TABLE IF EXISTS `recommendations`;

CREATE TABLE `recommendations` (
	`recommendation_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`condition_id`	BIGINT	NOT NULL,
	`admin_dong_id`	BIGINT	NOT NULL,
	`total_score`	DECIMAL(6,2)	NOT NULL,
	`data_coverage_rate`	DECIMAL(5,2)	NOT NULL,
	`rank`	INT	NOT NULL,
	`recommendation_reason`	TEXT	NOT NULL,
	`caution`	TEXT	NOT NULL,
	`status`	ENUM('VALID', 'INVALID')	NOT NULL	DEFAULT 'VALID',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`recommendation_id`),
	INDEX `idx_recommendations_condition_rank` (`condition_id`, `rank`)
);

DROP TABLE IF EXISTS `reviews`;

CREATE TABLE `reviews` (
	`review_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`admin_dong_id`	BIGINT	NOT NULL,
	`user_id`	BIGINT	NOT NULL,
	`overall_rating`	TINYINT	NOT NULL	DEFAULT 1,
	`content`	VARCHAR(500)	NOT NULL,
	`is_anonymous`	BOOLEAN	NOT NULL	DEFAULT FALSE,
	`status`	ENUM('ACTIVE', 'DELETED', 'HIDDEN')	NOT NULL	DEFAULT 'ACTIVE',
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`review_id`),
	INDEX `idx_reviews_admin_dong_status_created` (`admin_dong_id`, `status`, `created_at` DESC)
);

DROP TABLE IF EXISTS `review_admin_actions`;

CREATE TABLE `review_admin_actions` (
	`action_id`	BIGINT	NOT NULL AUTO_INCREMENT,
	`review_id`	BIGINT	NOT NULL,
	`action_type`	ENUM('HIDE', 'DELETE', 'RESTORE')	NOT NULL,
	`reason`	VARCHAR(255)	NOT NULL,
	`created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`action_id`)
);

-- FK: 사용자가 제공한 전체 FK 목록으로 교체 (범위 확장 승인됨, 관리 지역/사용자/추천/리뷰 참조 전체 반영)

ALTER TABLE `transit_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_transit_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `rental_transactions` ADD CONSTRAINT `FK_admin_dongs_TO_rental_transactions_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `recommendation_scores` ADD CONSTRAINT `FK_recommendations_TO_recommendation_scores_1` FOREIGN KEY (
	`recommendation_id`
)
REFERENCES `recommendations` (
	`recommendation_id`
);

ALTER TABLE `silence_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_silence_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `safety_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_safety_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `sports_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_sports_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `gu_crime_stats` ADD CONSTRAINT `FK_gus_TO_gu_crime_stats_1` FOREIGN KEY (
	`gu_id`
)
REFERENCES `gus` (
	`gu_id`
);

ALTER TABLE `nature_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_nature_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `review_reports` ADD CONSTRAINT `FK_reviews_TO_review_reports_1` FOREIGN KEY (
	`review_id`
)
REFERENCES `reviews` (
	`review_id`
);

ALTER TABLE `review_reports` ADD CONSTRAINT `FK_users_TO_review_reports_1` FOREIGN KEY (
	`reporter_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `review_reports` ADD CONSTRAINT `FK_users_TO_review_reports_2` FOREIGN KEY (
	`handler_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `property_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_property_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `preferred_house_types` ADD CONSTRAINT `FK_user_conditions_TO_preferred_house_types_1` FOREIGN KEY (
	`condition_id`
)
REFERENCES `user_conditions` (
	`condition_id`
);

ALTER TABLE `culture_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_culture_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `healthcare_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_healthcare_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `food_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_food_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `places` ADD CONSTRAINT `FK_admin_dongs_TO_places_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `favorite_dongs` ADD CONSTRAINT `FK_users_TO_favorite_dongs_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `favorite_dongs` ADD CONSTRAINT `FK_admin_dongs_TO_favorite_dongs_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `data_collection_logs` ADD CONSTRAINT `FK_data_sources_TO_data_collection_logs_1` FOREIGN KEY (
	`data_source_id2`
)
REFERENCES `data_sources` (
	`data_source_id`
);

ALTER TABLE `admin_dongs` ADD CONSTRAINT `FK_gus_TO_admin_dongs_1` FOREIGN KEY (
	`gu_id`
)
REFERENCES `gus` (
	`gu_id`
);

ALTER TABLE `condition_gus` ADD CONSTRAINT `FK_gus_TO_condition_gus_1` FOREIGN KEY (
	`gu_id`
)
REFERENCES `gus` (
	`gu_id`
);

ALTER TABLE `condition_gus` ADD CONSTRAINT `FK_user_conditions_TO_condition_gus_1` FOREIGN KEY (
	`condition_id`
)
REFERENCES `user_conditions` (
	`condition_id`
);

ALTER TABLE `condition_weights` ADD CONSTRAINT `FK_user_conditions_TO_condition_weights_1` FOREIGN KEY (
	`condition_id2`
)
REFERENCES `user_conditions` (
	`condition_id`
);

ALTER TABLE `review_scores` ADD CONSTRAINT `FK_reviews_TO_review_scores_1` FOREIGN KEY (
	`review_id`
)
REFERENCES `reviews` (
	`review_id`
);

ALTER TABLE `review_scores` ADD CONSTRAINT `FK_review_categories_TO_review_scores_1` FOREIGN KEY (
	`review_category_id`
)
REFERENCES `review_categories` (
	`review_category_id`
);

ALTER TABLE `condition_essentials` ADD CONSTRAINT `FK_user_conditions_TO_condition_essentials_1` FOREIGN KEY (
	`condition_id2`
)
REFERENCES `user_conditions` (
	`condition_id`
);

ALTER TABLE `life_convenience_metrics` ADD CONSTRAINT `FK_admin_dongs_TO_life_convenience_metrics_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `user_conditions` ADD CONSTRAINT `FK_users_TO_user_conditions_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `recommendations` ADD CONSTRAINT `FK_user_conditions_TO_recommendations_1` FOREIGN KEY (
	`condition_id`
)
REFERENCES `user_conditions` (
	`condition_id`
);

ALTER TABLE `recommendations` ADD CONSTRAINT `FK_admin_dongs_TO_recommendations_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `reviews` ADD CONSTRAINT `FK_admin_dongs_TO_reviews_1` FOREIGN KEY (
	`admin_dong_id`
)
REFERENCES `admin_dongs` (
	`admin_dong_id`
);

ALTER TABLE `reviews` ADD CONSTRAINT `FK_users_TO_reviews_1` FOREIGN KEY (
	`user_id`
)
REFERENCES `users` (
	`user_id`
);

ALTER TABLE `review_admin_actions` ADD CONSTRAINT `FK_reviews_TO_review_admin_actions_1` FOREIGN KEY (
	`review_id`
)
REFERENCES `reviews` (
	`review_id`
);
