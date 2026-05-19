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

    // 회원 추가 - 로그인/예약 테스트의 기반 데이터
    public Long insertMember(String email, String password, String name, String role) {
        jdbcTemplate.update(
                "INSERT INTO member (email, password, name, role) VALUES (?, ?, ?, ?)",
                email, password, name, role);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE email = ?",
                Long.class, email);
    }

    // 예약 추가 - name이 아니라 memberId 기준
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

    // 로그인 후 세션 쿠키(JSESSIONID)를 반환 - 이후 요청에 .cookie()로 첨부해 사용
    public String loginAndGetCookie(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post("/login")
                .then().statusCode(200)
                .extract().cookie("JSESSIONID");
    }
}
