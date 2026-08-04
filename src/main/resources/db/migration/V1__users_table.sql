CREATE TABLE users (
                       user_id            BIGINT NOT NULL AUTO_INCREMENT,
                       email              VARCHAR(255) UNIQUE,
                       password           VARCHAR(255),
                       name               VARCHAR(100),
                       nickname           VARCHAR(100) UNIQUE,
                       birth_date         DATE,
                       gender             ENUM('M', 'F', 'N') NOT NULL DEFAULT 'N',
                       profile_image_url  VARCHAR(500),

                       provider           ENUM('LOCAL', 'KAKAO') NOT NULL DEFAULT 'LOCAL',
                       provider_id        VARCHAR(100),

                       status             ENUM('ACTIVE', 'INACTIVE', 'WITHDRAWN')
                           NOT NULL DEFAULT 'ACTIVE',
                       role               ENUM('USER', 'ADMIN')
                           NOT NULL DEFAULT 'USER',

                       last_login_at      DATETIME,
                       created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at         DATETIME,

                       PRIMARY KEY (user_id),

                       CONSTRAINT uq_users_provider
                           UNIQUE (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;