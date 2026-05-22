-- 운영 시드 데이터
-- 운영 환경 진입 시 즉시 사용 가능한 최소한의 정적 데이터만 둠.
-- 더미 사용자/예약/매니저 데이터는 운영에 어울리지 않아 제외.
-- 테스트용 데이터는 각 테스트가 @BeforeEach에서 직접 준비.

-- [3단계] 매장 시드 2개 (테마가 store_id FK를 가지므로 먼저 삽입)
INSERT INTO store (name) VALUES ('강남점');
INSERT INTO store (name) VALUES ('홍대점');

-- 시간 슬롯 6개
INSERT INTO reservation_time (start_at) VALUES ('10:00');
INSERT INTO reservation_time (start_at) VALUES ('11:00');
INSERT INTO reservation_time (start_at) VALUES ('12:00');
INSERT INTO reservation_time (start_at) VALUES ('13:00');
INSERT INTO reservation_time (start_at) VALUES ('14:00');
INSERT INTO reservation_time (start_at) VALUES ('15:00');

-- [3단계] 테마 3개 — store_id 포함 (강남점 2개, 홍대점 1개)
INSERT INTO theme (store_id, name, description, thumbnail_url)
VALUES (1, '무인도 탈출', '갯벌이 많은 무인도를 탈출하는 흥미진진 대탈출!',
        'https://picsum.photos/seed/roomescape1/800/600.jpg');
INSERT INTO theme (store_id, name, description, thumbnail_url)
VALUES (1, '도시 탈출', '아포칼립스 상황인 도시 탈출하는 흥미진진 대탈출!',
        'https://picsum.photos/seed/roomescape2/800/600.jpg');
INSERT INTO theme (store_id, name, description, thumbnail_url)
VALUES (2, '열기구 탈출', '터지기 5분전! 열기구 탈출하는 흥미진진 대탈출!',
        'https://picsum.photos/seed/roomescape3/800/600.jpg');
