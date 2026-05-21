package roomescape;

import static org.hamcrest.Matchers.is;

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
 * 1단계 — 에러 응답 명세 통합 테스트.
 *
 * 모든 에러 응답이 {"message": "..."} 단일 필드 형식인지,
 * 상태 코드가 정책대로인지, 메시지가 명세대로인지 검증한다.
 *
 * [1단계 변경]
 * - name 필드 제거 → 예약 생성 body에서 name 삭제, 로그인 쿠키 추가
 * - 빈_이름, 이름_30자_초과 → name 필드 자체가 없어졌으므로 제거
 * - AuthError 중첩 클래스 추가 (비로그인 401, 잘못된 비밀번호 401)
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
        themeId = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg");
        cookie = helper.login("user@test.com", "pass");
    }

    // ──────── 1단계 신규: 인증 실패 ────────

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
            // 첫 예약을 DB에 직접 삽입
            helper.insertReservation(memberId, FUTURE_DATE, timeId10, themeId);

            // 같은 슬롯으로 두 번째 예약 시도
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
            // timeId 누락

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
            // themeId 누락

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
        @DisplayName("잘못된 JSON (date 형식 오류) → 400 + 메시지")
        void 잘못된_JSON_날짜_형식() {
            String malformedBody = """
                    {
                        "date": "2026/05/15",
                        "timeId": %d,
                        "themeId": %d
                    }
                    """.formatted(timeId10, themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(malformedBody)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("요청 본문의 형식이 올바르지 않습니다."));
        }
    }

    // ──────── 시간 삭제 에러 ────────

    @Nested
    @DisplayName("시간 삭제 시 에러 응답")
    class TimeDelete {

        @Test
        @DisplayName("예약이 존재하는 시간 삭제 시도 → 400 + 메시지")
        void 예약_존재하는_시간_삭제() {
            helper.insertReservation(memberId, FUTURE_DATE, timeId10, themeId);

            RestAssured.given().log().all()
                    .when().delete("/admin/times/" + timeId10)
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("예약이 존재하는 시간은 삭제할 수 없습니다."));
        }
    }

    // ──────── 요청 형식 오류 ────────

    @Nested
    @DisplayName("요청 형식 오류")
    class RequestFormat {

        @Test
        @DisplayName("필수 쿼리 파라미터(date) 누락 → 400 + 메시지")
        void date_쿼리_누락() {
            RestAssured.given().log().all()
                    .when().get("/user/themes/" + themeId + "/available-times")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("필수 요청 파라미터가 누락되었습니다."));
        }

        @Test
        @DisplayName("경로 변수 타입 오류 → 400 + 메시지")
        void 경로_변수_타입_오류() {
            RestAssured.given().log().all()
                    .when().get("/user/themes/abc/available-times?date=2026-05-15")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("요청 파라미터 형식이 올바르지 않습니다."));
        }
    }
}
