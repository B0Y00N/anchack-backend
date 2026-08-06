-- V4: 정적 지표 테이블(*_metrics)에서 data_date 컬럼 제거
-- 데이터 기준일은 data_sources 테이블에서 이미 관리하므로 중복 제거.
-- 지표 테이블들은 시계열로 계속 쌓이는 구조라, data_date 대신 created_at 기준으로
-- "행정동별 최신 스냅샷"을 조회할 수 있도록 인덱스를 교체한다.
--
-- 범위: places 기반 카운트 계산 대상인 8개 테이블(culture, transit, sports, nature,
-- life_convenience, healthcare, food, safety) + silence_metrics + safety_metrics가
-- 참조하는 gu_crime_stats. gu_crime_stats는 인덱스 교체 없이 컬럼만 제거
-- (조회 빈도가 낮고 gu_code당 행 수가 적어 인덱스 필요성이 낮음).
-- property_metrics는 이번 작업 범위 밖이라 손대지 않음 - 같은 결정이 적용되어야 하면 별도 확인 필요.

ALTER TABLE `transit_metrics`
	DROP INDEX `idx_transit_metrics_admin_dong_data_date`,
	DROP COLUMN `data_date`,
	ADD INDEX `idx_transit_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `safety_metrics`
	DROP INDEX `idx_safety_metrics_admin_dong_data_date`,
	DROP COLUMN `data_date`,
	ADD INDEX `idx_safety_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `sports_metrics`
	DROP INDEX `idx_sports_metrics_admin_dong_data_date`,
	DROP COLUMN `data_date`,
	ADD INDEX `idx_sports_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `nature_metrics`
	DROP INDEX `idx_nature_metrics_admin_dong_data_date`,
	DROP COLUMN `data_date`,
	ADD INDEX `idx_nature_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `culture_metrics`
	DROP INDEX `idx_culture_metrics_admin_dong_data_date`,
	DROP COLUMN `data_date`,
	ADD INDEX `idx_culture_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `life_convenience_metrics`
	DROP INDEX `idx_life_convenience_metrics_admin_dong_data_date`,
	DROP COLUMN `data_date`,
	ADD INDEX `idx_life_convenience_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `healthcare_metrics`
	DROP COLUMN `data_date`,
	ADD INDEX `idx_healthcare_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `food_metrics`
	DROP COLUMN `data_date`,
	ADD INDEX `idx_food_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `silence_metrics`
	DROP COLUMN `data_date`,
	ADD INDEX `idx_silence_metrics_admin_dong_created` (`admin_dong_id`, `created_at` DESC);

ALTER TABLE `gu_crime_stats`
	DROP COLUMN `data_date`;
