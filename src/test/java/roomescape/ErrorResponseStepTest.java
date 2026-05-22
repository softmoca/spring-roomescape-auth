package roomescape;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import roomescape.domain.policy.ReservationPolicy;
import roomescape.support.ReservationTestHelper;
import roomescape.support.TestFutureOnlyPolicy;

/*
 * 에러 응답 명세 통합 테스트.
 * 모든 에러 응답이 {"message": "..."} 단일 필드 형식인지 검증한다.
 *
 * [1단계 변경]
 * - name 필드 제거, 로그인 쿠키 추가
 * - 빈_이름, 이름_30자_초과 제거
 * - AuthError 중첩 클래스 추가
 *
 * [3단계 변경]
 * - setUp(): insertStore() + insertTheme(storeId) 적용
 * - AuthzError 중첩 클래스 추가 (인가 실패 케이스)
 */
public class ErrorResponseStepTest extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 13);
    private static final LocalTime NOW_TIME = LocalTime.of(12, 0);
    private static final LocalDate FUTURE_DATE = TODAY.plusDays(7);

    @TestConfiguration
    static class FixedPolicyConfig {
        @Bean
        @Primary
        public ReservationPolicy fixedReservationPolicy() {
            Clock fixed = Clock.fixed(
                    TODAY.atTime(NOW_TIME).atZone(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault()
            );
            return new TestFutureOnlyPolicy(fixed);
        }
    }

    @Autowired
    private ReservationTestHelper helper;

    private Long memberId;
    private Long timeId10;
    private Long themeId;
    private String cookie;

    @BeforeEach
    void setUp() {
        memberId = helper.insertMember("user@test.com", "pass", "사용자");
        timeId10 = helper.insertTime(LocalTime.of(10, 0));
        Long storeId = helper.insertStore("에러테스트매장");
        themeId  = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg", storeId);
        cookie   = helper.login("user@test.com", "pass");
    }

    // ──────── 인증 실패 ────────

    @Nested
    @DisplayName("인증 실패 에러 응답")
    class AuthError {

        @Test
        @DisplayName("비로그인 예약 생성 시도 → 401 + 메시지")
        void 비로그인_예약_생성() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", timeId10);
            body.put("themeId", themeId);

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("로그인이 필요합니다."));
        }

        @Test
        @DisplayName("잘못된 비밀번호 → 401 + 메시지")
        void 잘못된_로그인_정보() {
            Map<String, String> body = new HashMap<>();
            body.put("email", "user@test.com");
            body.put("password", "wrongpassword");

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → 401 + 동일 메시지 (정보 노출 방지)")
        void 존재하지_않는_이메일() {
            Map<String, String> body = new HashMap<>();
            body.put("email", "nobody@test.com");
            body.put("password", "pass");

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    // ──────── 3단계 신규: 인가 실패 ────────

    @Nested
    @DisplayName("인가 실패 에러 응답")
    class AuthzError {

        @Test
        @DisplayName("매니저 아닌 사용자가 /admin/reservations 접근 → 403 + message 포함")
        void 매니저_아님_403() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .when().get("/admin/reservations")
                    .then().log().all()
                    .statusCode(403)
                    .body("message", notNullValue());
        }

        @Test
        @DisplayName("비로그인 /admin/reservations 접근 → 401")
        void 비로그인_어드민_접근_401() {
            RestAssured.given().log().all()
                    .when().get("/admin/reservations")
                    .then().log().all()
                    .statusCode(401);
        }
    }

    // ──────── 예약 생성 에러 ────────

    @Nested
    @DisplayName("예약 생성 시 에러 응답")
    class ReservationCreate {

        @Test
        @DisplayName("과거 날짜, 시간 예약 시도 → 400 + 메시지")
        void 과거_시점_예약() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", TODAY.minusDays(1).toString());
            body.put("timeId", timeId10);
            body.put("themeId", themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("지나간 날짜, 시간으로는 예약할 수 없습니다."));
        }

        @Test
        @DisplayName("중복 예약 시도 → 400 + 메시지")
        void 중복_예약() {
            helper.insertReservation(memberId, FUTURE_DATE, timeId10, themeId);

            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", timeId10);
            body.put("themeId", themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("해당 시간은 이미 예약되었습니다. 다른 시간을 선택해 주세요."));
        }

        @Test
        @DisplayName("존재하지 않는 시간 ID → 404 + 메시지")
        void 존재하지_않는_시간() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", 9999L);
            body.put("themeId", themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(404)
                    .body("message", is("존재하지 않는 시간입니다."));
        }

        @Test
        @DisplayName("존재하지 않는 테마 ID → 404 + 메시지")
        void 존재하지_않는_테마() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", timeId10);
            body.put("themeId", 9999L);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(404)
                    .body("message", is("존재하지 않는 테마입니다."));
        }

        @Test
        @DisplayName("timeId 누락 → 400 + 메시지")
        void timeId_누락() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("themeId", themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("예약 시간을 선택해 주세요."));
        }

        @Test
        @DisplayName("themeId 누락 → 400 + 메시지")
        void themeId_누락() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", timeId10);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("예약 테마를 선택해 주세요."));
        }

        @Test
        @DisplayName("잘못된 JSON 날짜 형식 → 400 + 메시지")
        void 잘못된_JSON_날짜_형식() {
            String raw = "{\"date\":\"not-a-date\",\"timeId\":" + timeId10 + ",\"themeId\":" + themeId + "}";

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(raw)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", notNullValue());
        }
    }
}
