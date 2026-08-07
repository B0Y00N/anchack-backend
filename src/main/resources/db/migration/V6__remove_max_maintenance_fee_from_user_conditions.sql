-- V6: 동적 점수 계산(recommendation) 도메인 착수를 위한 ERD 반영
-- 반영 사항:
--   1) condition_weights/recommendation_scores.category: SHOPPING 삭제,
--      CONVENIENCE(life_convenience_metrics 대응)·HEALTHCARE 추가,
--      SILENT/SILENCE 표기를 SILENCE로 통일
--   2) places.category: GYM 추가(조건 필수 인프라 '헬스장' 매핑용),
--      CONVENIENT_STORE -> CONVENIENCE_STORE 오타 수정
--   3) recommendations: 통근 시간/환승 횟수 컬럼 추가, status 컬럼 삭제
--      (재계산 시 기존 행을 DELETE 후 INSERT하는 방식으로 대체하기로 함)
--   4) user_conditions: is_latest(최신 데이터 갱신 여부), is_saved(저장된 조건 여부) 추가
--   5) property_metrics/rental_transactions/preferred_house_types.house_type:
--      '단독다가구'(또는 '단독 다가구')를 '단독'/'다가구'로 분리
--   6) property_metrics: min_area/max_area -> avg_area 단일 컬럼으로 통합
--      (예산·평수 조건은 소프트 필터가 아닌 하드 필터로 재논의됨 - 애플리케이션 로직에서 반영)
--   7) user_conditions에서 관리비 조건 컬럼 제거
-- 대상 테이블이 로컬 환경에서 전부 비어 있음을 확인 후 작성함(데이터 마이그레이션 불필요).

ALTER TABLE `condition_weights`
    MODIFY COLUMN `category` ENUM('TRANSIT', 'SAFETY', 'SPORTS', 'FOOD', 'CONVENIENCE', 'HEALTHCARE', 'CULTURE', 'NATURE', 'SILENCE') NOT NULL;

ALTER TABLE `recommendation_scores`
    MODIFY COLUMN `category` ENUM('TRANSIT', 'SAFETY', 'SPORTS', 'FOOD', 'CONVENIENCE', 'HEALTHCARE', 'CULTURE', 'NATURE', 'SILENCE') NOT NULL;

ALTER TABLE `places`
    MODIFY COLUMN `category` ENUM('SPORTS', 'RIVER', 'TRAIL', 'PARK', 'CULTURE', 'BUS_STOP', 'SUBWAY_STATION', 'POLICE', 'STREET_LIGHT', 'SAFETY_BELL', 'CCTV', 'MART', 'DEPARTMENT_STORE', 'HOSPITAL', 'PHARMACY', 'BANK', 'CAFE', 'RESTAURANT', 'CONVENIENCE_STORE', 'TOWN_OFFICE', 'GYM') NOT NULL;

ALTER TABLE `recommendations`
    ADD COLUMN `commute_time` INT NULL AFTER `data_coverage_rate`,
    ADD COLUMN `transfer_count` INT NULL AFTER `commute_time`,
    DROP COLUMN `status`;

ALTER TABLE `user_conditions`
    ADD COLUMN `is_latest` BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN `is_saved` BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE `property_metrics`
    MODIFY COLUMN `house_type` ENUM('오피스텔', '빌라', '단독', '다가구', '아파트', '원룸') NOT NULL,
    DROP COLUMN `min_area`,
    DROP COLUMN `max_area`,
    ADD COLUMN `avg_area` DECIMAL(5,2) NOT NULL DEFAULT 0 AFTER `house_type`;

ALTER TABLE `rental_transactions`
    MODIFY COLUMN `house_type` ENUM('오피스텔', '빌라', '단독', '다가구', '아파트', '원룸') NOT NULL;

ALTER TABLE `preferred_house_types`
    MODIFY COLUMN `house_type` ENUM('오피스텔', '빌라', '단독', '다가구', '아파트', '원룸') NOT NULL;

ALTER TABLE `user_conditions`
    DROP COLUMN `max_maintenance_fee`;
