drop table if exists reservation;
drop table if exists reservation_time;
drop table if exists theme;
drop table if exists member;

CREATE TABLE member (
                        id       BIGINT       NOT NULL AUTO_INCREMENT,
                        email    VARCHAR(255) NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        name     VARCHAR(30)  NOT NULL,
                        role     VARCHAR(20)  NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE (email)
);

CREATE TABLE reservation_time (
                                  id       BIGINT       NOT NULL AUTO_INCREMENT,
                                  start_at TIME NOT NULL,
                                  PRIMARY KEY (id),
                                  UNIQUE (start_at)
);

CREATE TABLE theme (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(30) NOT NULL ,
  description   VARCHAR(255) NOT NULL ,
  thumbnail_url  VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE reservation (
                             id      BIGINT       NOT NULL AUTO_INCREMENT,
                             member_id BIGINT NOT NULL,
                             date    DATE NOT NULL,
                             time_id BIGINT NOT NULL,
                             theme_id BIGINT NOT NULL,
                             PRIMARY KEY (id),
                             FOREIGN KEY (member_id) REFERENCES member (id),
                             FOREIGN KEY (time_id) REFERENCES reservation_time (id),
                             FOREIGN KEY (theme_id) REFERENCES theme (id),
                             UNIQUE (date, time_id, theme_id)
);
