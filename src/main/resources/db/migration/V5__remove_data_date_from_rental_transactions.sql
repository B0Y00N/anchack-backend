-- V5: 국토부 전월세 거래에서 신뢰할 수 있는 자료 기준일이 없는 data_date 컬럼 제거

ALTER TABLE `rental_transactions`
	DROP COLUMN `data_date`;

ALTER TABLE `property_metrics`
    DROP COLUMN `data_date`;
