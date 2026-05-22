drop table if exists reservation;
drop table if exists manager;
drop table if exists member;
drop table if exists reservation_time;
drop table if exists theme;
drop table if exists store;

CREATE TABLE store (
                       id   BIGINT      NOT NULL AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL,
                       PRIMARY KEY (id)
);

CREATE TABLE reservation_time (
                                  id       BIGINT NOT NULL AUTO_INCREMENT,
                                  start_at TIME   NOT NULL,
                                  PRIMARY KEY (id),
                                  UNIQUE (start_at)
);

CREATE TABLE theme (
                       id            BIGINT       NOT NULL AUTO_INCREMENT,
                       store_id      BIGINT       NOT NULL,
                       name          VARCHAR(30)  NOT NULL,
                       description   VARCHAR(255) NOT NULL,
                       thumbnail_url VARCHAR(255) NOT NULL,
                       PRIMARY KEY (id),
                       FOREIGN KEY (store_id) REFERENCES store (id)
);

CREATE TABLE member (
                        id       BIGINT       NOT NULL AUTO_INCREMENT,
                        email    VARCHAR(255) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        name     VARCHAR(30)  NOT NULL,
                        PRIMARY KEY (id)
);

CREATE TABLE manager (
                         id        BIGINT NOT NULL AUTO_INCREMENT,
                         member_id BIGINT NOT NULL UNIQUE,  -- 한 회원은 하나의 매장만 담당 (1:1 시작)
                         store_id  BIGINT NOT NULL,
                         PRIMARY KEY (id),
                         FOREIGN KEY (member_id) REFERENCES member (id),
                         FOREIGN KEY (store_id)  REFERENCES store (id)
);

CREATE TABLE reservation (
                             id        BIGINT NOT NULL AUTO_INCREMENT,
                             member_id BIGINT NOT NULL,
                             date      DATE   NOT NULL,
                             time_id   BIGINT NOT NULL,
                             theme_id  BIGINT NOT NULL,
                             PRIMARY KEY (id),
                             FOREIGN KEY (member_id) REFERENCES member (id),
                             FOREIGN KEY (time_id)   REFERENCES reservation_time (id),
                             FOREIGN KEY (theme_id)  REFERENCES theme (id),
                             UNIQUE (date, time_id, theme_id)
);
