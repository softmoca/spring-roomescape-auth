package roomescape;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
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
 * 내 예약 관리 API 통합 테스트.
 *
 * [1단계 변경]
 * - ?name= 파라미터 기반 → 로그인 세션 쿠키 기반으로 전환
 * - setUp()에 insertMember() + login() 추가
 * - 비로그인 → 401 케이스 추가
 *
 * [3단계 변경]
 * - setUp(): insertStore() + insertTheme(storeId) 적용
 */
public class MyReservationStepTest extends IntegrationTest {

    private static final LocalDate FUTURE_DATE_1 = LocalDate.of(2050, 5, 15);
    private static final LocalDate FUTURE_DATE_2 = LocalDate.of(2050, 5, 20);

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 13);
    private static final LocalTime NOW_TIME = LocalTime.of(12, 0);

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

    private Long brownId;
    private Long konId;
    private Long timeId10;
    private Long timeId11;
    private Long themeId;
    private String brownCookie;
    private String konCookie;

    @BeforeEach
    void setUp() {
        brownId = helper.insertMember("brown@test.com", "pass1", "브라운");
        konId   = helper.insertMember("kon@test.com",   "pass2", "콘");
        timeId10 = helper.insertTime(LocalTime.of(10, 0));
        timeId11 = helper.insertTime(LocalTime.of(11, 0));
        // 3단계: insertStore + insertTheme(storeId)
        Long storeId = helper.insertStore("내예약테스트매장");
        themeId = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg", storeId);
        brownCookie = helper.login("brown@test.com", "pass1");
        konCookie   = helper.login("kon@test.com",   "pass2");
    }

    // ──────── 내 예약 조회 ────────

    @Nested
    @DisplayName("내 예약 조회")
    class MyReservationList {

        @Test
        @DisplayName("내 예약 목록을 날짜, 시간 순으로 반환한다")
        void 내_예약_조회() {
            helper.insertReservation(brownId, FUTURE_DATE_2, timeId10, themeId);
            helper.insertReservation(brownId, FUTURE_DATE_1, timeId11, themeId);
            helper.insertReservation(konId, FUTURE_DATE_1, timeId10, themeId); // 필터링 검증

            ExtractableResponse<Response> response = RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .when().get("/user/reservations")
                    .then().log().all()
                    .statusCode(200)
                    .body("size()", is(2))
                    .extract();

            List<String> dates = response.jsonPath().getList("date");
            assert dates.get(0).equals(FUTURE_DATE_1.toString());
            assert dates.get(1).equals(FUTURE_DATE_2.toString());
        }

        @Test
        @DisplayName("같은 날짜에 여러 예약이 있으면 시간 순으로 정렬된다")
        void 같은_날짜_시간_정렬() {
            helper.insertReservation(brownId, FUTURE_DATE_1, timeId11, themeId);
            helper.insertReservation(brownId, FUTURE_DATE_1, timeId10, themeId);

            ExtractableResponse<Response> response = RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .when().get("/user/reservations")
                    .then().statusCode(200).extract();

            List<String> times = response.jsonPath().getList("time.startAt");
            assert times.get(0).equals("10:00");
            assert times.get(1).equals("11:00");
        }

        @Test
        @DisplayName("예약이 없으면 빈 배열을 반환한다")
        void 예약_없으면_빈_배열() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .when().get("/user/reservations")
                    .then().log().all()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        @DisplayName("비로그인 조회 시도 → 401")
        void 비로그인_조회_시도() {
            RestAssured.given().log().all()
                    .when().get("/user/reservations")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("로그인이 필요합니다."));
        }
    }

    // ──────── 내 예약 취소 ────────

    @Nested
    @DisplayName("내 예약 취소")
    class MyReservationCancel {

        @Test
        @DisplayName("본인의 미래 예약을 취소하면 204를 반환하고 실제로 삭제된다")
        void 본인_미래_예약_취소() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .when().delete("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(204);

            RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .when().get("/user/reservations")
                    .then().statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        @DisplayName("존재하지 않는 예약 ID로 취소 시도 → 404")
        void 존재하지_않는_예약() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .when().delete("/user/reservations/9999")
                    .then().log().all()
                    .statusCode(404)
                    .body("message", is("존재하지 않는 예약입니다."));
        }

        @Test
        @DisplayName("다른 사람의 예약 취소 시도 → 404 (정보 노출 방지)")
        void 다른_사람의_예약() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", konCookie)
                    .when().delete("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(404)
                    .body("message", is("존재하지 않는 예약입니다."));
        }

        @Test
        @DisplayName("이미 지난 예약 취소 시도 → 400")
        void 이미_지난_예약() {
            LocalDate yesterday = TODAY.minusDays(1);
            Long reservationId = helper.insertReservationAndReturnId(brownId, yesterday, timeId10, themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .when().delete("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("이미 지난 예약은 취소할 수 없습니다."));
        }

        @Test
        @DisplayName("비로그인 취소 시도 → 401")
        void 비로그인_취소_시도() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            RestAssured.given().log().all()
                    .when().delete("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(401);
        }
    }

    // ──────── 내 예약 변경 ────────

    @Nested
    @DisplayName("내 예약 변경")
    class MyReservationUpdate {

        @Test
        @DisplayName("본인의 미래 예약을 변경하면 200과 변경된 예약 정보를 반환한다")
        void 본인_예약_변경_성공() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE_2.toString());
            body.put("timeId", timeId11);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().patch("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(200)
                    .body("date", is(FUTURE_DATE_2.toString()))
                    .body("time.id", is(timeId11.intValue()));
        }

        @Test
        @DisplayName("새 시간이 이미 지난 시간이면 → 400")
        void 새_시간이_과거() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            Map<String, Object> body = new HashMap<>();
            body.put("date", TODAY.minusDays(1).toString());
            body.put("timeId", timeId10);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().patch("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("지나간 날짜, 시간으로는 변경할 수 없습니다."));
        }

        @Test
        @DisplayName("변경하려는 시간이 다른 사람에 의해 예약됨 → 400")
        void 시간_충돌() {
            Long myReservation = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);
            helper.insertReservation(konId, FUTURE_DATE_1, timeId11, themeId);

            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE_1.toString());
            body.put("timeId", timeId11);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().patch("/user/reservations/" + myReservation)
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("해당 시간은 이미 예약되었습니다. 다른 시간을 선택해 주세요."));
        }

        @Test
        @DisplayName("존재하지 않는 timeId로 변경 시도 → 404")
        void 존재하지_않는_시간() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE_2.toString());
            body.put("timeId", 9999L);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", brownCookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().patch("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(404)
                    .body("message", is("존재하지 않는 시간입니다."));
        }

        @Test
        @DisplayName("비로그인 변경 시도 → 401")
        void 비로그인_변경_시도() {
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE_1, timeId10, themeId);

            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE_2.toString());
            body.put("timeId", timeId11);

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().patch("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(401);
        }
    }

    // ──────── 예약 생애주기 시나리오 ────────

    @Nested
    @DisplayName("내 예약 생애주기 시나리오")
    class MyReservationLifecycle {

        @Test
        @DisplayName("예약 생성 → 조회 → 변경 → 취소 흐름이 자연스럽게 이어진다")
        void 예약_생애주기() {
            // 1) 예약 생성
            Map<String, Object> createBody = new HashMap<>();
            createBody.put("date", FUTURE_DATE_1.toString());
            createBody.put("timeId", timeId10);
            createBody.put("themeId", themeId);

            Long reservationId = RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when().post("/user/reservations")
                    .then().statusCode(201)
                    .extract().jsonPath().getLong("id");

            // 2) 조회 — 1건
            RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .when().get("/user/reservations")
                    .then().statusCode(200)
                    .body("size()", is(1));

            // 3) 변경
            Map<String, Object> patchBody = new HashMap<>();
            patchBody.put("date", FUTURE_DATE_2.toString());
            patchBody.put("timeId", timeId11);

            RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .contentType(ContentType.JSON)
                    .body(patchBody)
                    .when().patch("/user/reservations/" + reservationId)
                    .then().statusCode(200)
                    .body("date", is(FUTURE_DATE_2.toString()));

            // 4) 취소
            RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .when().delete("/user/reservations/" + reservationId)
                    .then().statusCode(204);

            // 5) 취소 후 조회 — 0건
            RestAssured.given()
                    .cookie("JSESSIONID", brownCookie)
                    .when().get("/user/reservations")
                    .then().statusCode(200)
                    .body("size()", is(0));
        }
    }
}
