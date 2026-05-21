package roomescape.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
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

    public Long insertTheme(String name, String description, String thumbnailUrl) {
        jdbcTemplate.update(
                "INSERT INTO theme (name, description, thumbnail_url) VALUES (?, ?, ?)",
                name, description, thumbnailUrl);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM theme WHERE name = ?",
                Long.class, name);
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
     * RestAssured로 로그인 요청을 보내고 세션 쿠키를 반환한다.
     * 테스트에서 인증이 필요한 API 호출 시 .cookie(helper.login(...)) 형태로 사용.
     */
    public String login(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/login")
                .then().statusCode(200)
                .extract().response();

        return response.cookie("JSESSIONID");
    }
}
