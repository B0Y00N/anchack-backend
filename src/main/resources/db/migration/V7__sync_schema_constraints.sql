-- V7: Synchronize schema constraints and column policies with the current ERD.

-- =====================================================================
-- condition tables: condition_id2 -> condition_id
-- =====================================================================

ALTER TABLE `condition_weights` DROP FOREIGN KEY `FK_user_conditions_TO_condition_weights_1`;
ALTER TABLE `condition_weights` DROP PRIMARY KEY;
ALTER TABLE `condition_weights` CHANGE COLUMN `condition_id2` `condition_id` BIGINT NOT NULL;
ALTER TABLE `condition_weights` ADD PRIMARY KEY (`category`, `condition_id`);
ALTER TABLE `condition_weights`
    ADD CONSTRAINT `FK_user_conditions_TO_condition_weights_1`
    FOREIGN KEY (`condition_id`) REFERENCES `user_conditions` (`condition_id`);

ALTER TABLE `condition_essentials` DROP FOREIGN KEY `FK_user_conditions_TO_condition_essentials_1`;
ALTER TABLE `condition_essentials` DROP PRIMARY KEY;
ALTER TABLE `condition_essentials` CHANGE COLUMN `condition_id2` `condition_id` BIGINT NOT NULL;
ALTER TABLE `condition_essentials` ADD PRIMARY KEY (`category`, `condition_id`);
ALTER TABLE `condition_essentials`
    ADD CONSTRAINT `FK_user_conditions_TO_condition_essentials_1`
    FOREIGN KEY (`condition_id`) REFERENCES `user_conditions` (`condition_id`);

-- =====================================================================
-- data_collection_logs
-- =====================================================================

ALTER TABLE `data_collection_logs`
    ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- =====================================================================
-- data_sources
-- =====================================================================

ALTER TABLE `data_sources`
    MODIFY COLUMN `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    DROP COLUMN `data_date`,
    DROP COLUMN `updated_at`;

-- =====================================================================
-- gus: code remains the primary key, name is no longer unique.
-- =====================================================================

ALTER TABLE `gus` DROP INDEX `uq_gus_name`;

-- =====================================================================
-- places
-- =====================================================================

UPDATE `places`
SET `external_id` = SHA2(
        CONCAT_WS('|', `data_source_id`, `category`, `name`, `latitude`, `longitude`, `place_id`),
        256
    )
WHERE `external_id` IS NULL OR TRIM(`external_id`) = '';

ALTER TABLE `places`
    MODIFY COLUMN `external_id` VARCHAR(200) NOT NULL,
    DROP COLUMN `data_date`,
    ADD UNIQUE KEY `uq_places_data_source_external` (`data_source_id`, `external_id`);

-- =====================================================================
-- house_type enum policy
-- =====================================================================

ALTER TABLE `preferred_house_types`
MODIFY COLUMN `house_type`
    ENUM(
        '오피스텔',
        '연립',
        '다세대',
        '연립다세대',
        '단독',
        '다가구',
        '빌라',
        '아파트',
        '원룸'
    ) NOT NULL;

ALTER TABLE `property_metrics`
MODIFY COLUMN `house_type`
    ENUM(
        '오피스텔',
        '연립',
        '다세대',
        '연립다세대',
        '단독',
        '다가구',
        '빌라',
        '아파트',
        '원룸'
    ) NOT NULL,
MODIFY COLUMN `avg_rent` BIGINT NULL;

ALTER TABLE `rental_transactions`
MODIFY COLUMN `house_type`
    ENUM(
        '오피스텔',
        '연립',
        '다세대',
        '연립다세대',
        '단독',
        '다가구'
    ) NOT NULL;

-- =====================================================================
-- rental_transactions
-- =====================================================================

ALTER TABLE `rental_transactions`
    CHANGE COLUMN `deposit` `deposit_amount` BIGINT NOT NULL DEFAULT 0,
    CHANGE COLUMN `rent` `monthly_rent_amount` INT NOT NULL DEFAULT 0,
    ADD COLUMN `rental_type` ENUM('전세', '월세') NULL AFTER `legal_dong_name`,
    DROP COLUMN `maintenance_fee`;

UPDATE `rental_transactions`
SET `rental_type` = CASE
    WHEN `monthly_rent_amount` = 0 THEN '전세'
    ELSE '월세'
END;

ALTER TABLE `rental_transactions`
    MODIFY COLUMN `rental_type` ENUM('전세', '월세') NOT NULL;

-- =====================================================================
-- reviews
-- =====================================================================

ALTER TABLE `reviews`
    MODIFY COLUMN `is_anonymous` BOOLEAN NOT NULL DEFAULT FALSE;

-- =====================================================================
-- user_conditions
-- =====================================================================

UPDATE `user_conditions`
SET `max_deposit` = 0
WHERE `max_deposit` IS NULL;

ALTER TABLE `user_conditions`
    MODIFY COLUMN `max_deposit` BIGINT NOT NULL,
    MODIFY COLUMN `is_latest` BOOLEAN NOT NULL DEFAULT TRUE,
    MODIFY COLUMN `is_saved` BOOLEAN NOT NULL DEFAULT FALSE;
