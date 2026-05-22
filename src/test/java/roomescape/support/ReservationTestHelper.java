package roomescape.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationTestHelper {

    private final JdbcTemplate jdbcTemplate;

    public ReservationTestHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ──────── 기본 픽스처 ────────

    public Long insertTime(LocalTime startAt) {
        jdbcTemplate.update(
                "INSERT INTO reservation_time (start_at) VALUES (?)",
                Time.valueOf(startAt));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation_time WHERE start_at = ?",
                Long.class, Time.valueOf(startAt));
    }

    // ── 3단계: storeId 필수 오버로드 ──
    public Long insertTheme(String name, Long storeId) {
        return insertTheme(name, "테스트 설명", "https://example.com/thumb.jpg", storeId);
    }

    public Long insertTheme(String name, String description, String thumbnailUrl, Long storeId) {
        jdbcTemplate.update(
                "INSERT INTO theme (store_id, name, description, thumbnail_url) VALUES (?, ?, ?, ?)",
                storeId, name, description, thumbnailUrl);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ? AND store_id = ?",
                Long.class, name, storeId);
    }

    // ── 3단계: 신규 ──
    public Long insertStore(String name) {
        jdbcTemplate.update("INSERT INTO store (name) VALUES (?)", name);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM store WHERE name = ?",
                Long.class, name);
    }

    public Long insertManager(Long memberId, Long storeId) {
        jdbcTemplate.update(
                "INSERT INTO manager (member_id, store_id) VALUES (?, ?)",
                memberId, storeId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM manager WHERE member_id = ?",
                Long.class, memberId);
    }

    /** 회원 삽입 후 ID 반환 */
    public Long insertMember(String email, String password, String name) {
        jdbcTemplate.update(
                "INSERT INTO member (email, password, name) VALUES (?, ?, ?)",
                email, password, name);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE email = ?",
                Long.class, email);
    }

    /** memberId 기반 예약 삽입 */
    public void insertReservation(Long memberId, LocalDate date, Long timeId, Long themeId) {
        jdbcTemplate.update(
                "INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, ?, ?)",
                memberId, Date.valueOf(date), timeId, themeId);
    }

    public Long insertReservationAndReturnId(Long memberId, LocalDate date, Long timeId, Long themeId) {
        insertReservation(memberId, date, timeId, themeId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation WHERE member_id = ? AND date = ? AND time_id = ? AND theme_id = ?",
                Long.class, memberId, Date.valueOf(date), timeId, themeId);
    }

    // ──────── 로그인 헬퍼 ────────

    /**
     * RestAssured로 로그인 요청을 보내고 JSESSIONID 쿠키값(String)을 반환한다.
     * 기존 코드와의 호환성 유지 — String 반환.
     * 테스트에서: .cookie("JSESSIONID", helper.login(...)) 형태로 사용.
     */
    public String login(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/login")
                .then()
                .extract()
                .cookie("JSESSIONID");
    }
}
