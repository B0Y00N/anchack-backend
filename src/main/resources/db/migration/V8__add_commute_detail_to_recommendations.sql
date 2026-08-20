-- V8: 저장된 조건의 결과(추천 리스트) 조회 시 통근 상세를 복원할 수 있도록
-- recommendations에 route/transportType/lineNum/vehicleType/walkMin/transitMin을 추가한다.
--
-- 지금까지 이 값들은 조건 생성 시점에만 응답으로 나가고 DB에는 저장되지 않아서,
-- 저장된 조건을 나중에 다시 조회하면(GET /api/user-conditions/{conditionId}/recommendations)
-- commuteTime/transferCount만 남고 나머지는 전부 null이었다. commute_time/transfer_count와
-- 동일하게 생성 시점에 그대로 저장해서 재계산(카카오 API 재호출) 없이 복원한다.
--
-- route는 환승이 있으면 여러 구간이 ", "로 이어붙어 recommendation_reason/caution만큼
-- 길어질 수 있어 TEXT로 둔다. 나머지는 짧은 코드/이름값이라 VARCHAR로 충분하다.

ALTER TABLE `recommendations`
    ADD COLUMN `route` TEXT NULL AFTER `transfer_count`,
    ADD COLUMN `transport_type` VARCHAR(20) NULL AFTER `route`,
    ADD COLUMN `line_num` VARCHAR(50) NULL AFTER `transport_type`,
    ADD COLUMN `vehicle_type` VARCHAR(20) NULL AFTER `line_num`,
    ADD COLUMN `walk_min` INT NULL AFTER `vehicle_type`,
    ADD COLUMN `transit_min` INT NULL AFTER `walk_min`;
