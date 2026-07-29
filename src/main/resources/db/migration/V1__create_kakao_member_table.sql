CREATE TABLE kakao_member (
      id             BIGINT       NOT NULL AUTO_INCREMENT,
      kakao_id       BIGINT       NOT NULL,
      nickname       VARCHAR(100),
      profile_image  VARCHAR(500),
      email          VARCHAR(200),
      created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      CONSTRAINT uq_kakao_member_kakao_id UNIQUE (kakao_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
