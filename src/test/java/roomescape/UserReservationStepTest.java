package roomescape;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/*
 * 1단계 — 사용자 예약 API 요구사항 테스트.
 *
 * IntegrationTest 상속으로 매 테스트 빈 DB 보장.
 * 미래 날짜를 사용하므로 시계 통제는 불필요.
 *
 * [1단계 변경]
 * - setUp()에 insertMember() + login() 추가
 * - 예약 생성 body: name 제거, 로그인 쿠키 추가
 * - 예약 생성 응답 검증: "name" → "memberName"
 * - 같은_시간_다른_테마 테스트도 동일하게 쿠키 기반으로 수정
 */

/*
 * 사용자 예약 API 요구사항 테스트.
 * IntegrationTest 상속으로 매 테스트 빈 DB 보장.
 * 미래 날짜를 사용하므로 시계 통제는 불필요.
 *
 * [1단계 변경]
 * - setUp()에 insertMember() + login() 추가
 * - 예약 생성 body: name 제거, 로그인 쿠키 추가
 * - 예약 생성 응답 검증: "name" → "memberName"
 *  * - 같은_시간_다른_테마 테스트도 동일하게 쿠키 기반으로 수정
 *
 * [3단계 변경]
 * - setUp(): insertStore() + insertTheme(storeId) 적용
 */
public class UserReservationStepTest extends IntegrationTest {

    private static final String FUTURE_DATE = "2050-12-31";

    @Autowired
    private ReservationTestHelper helper;

    private Long timeId;
    private Long themeIdA;
    private Long themeIdB;
    private String brownCookie;
    private String konCookie;

    @BeforeEach
    void setUp() {
        helper.insertMember("brown@test.com", "pass1", "브라운");
        helper.insertMember("kon@test.com", "pass2", "콘");
        timeId = helper.insertTime(LocalTime.of(10, 0));
        // 3단계: insertStore + insertTheme(storeId)
        Long storeId = helper.insertStore("사용자테스트매장");
        themeIdA = helper.insertTheme("테마A", "설명A", "https://example.com/a.jpg", storeId);
        themeIdB = helper.insertTheme("테마B", "설명B", "https://example.com/b.jpg", storeId);
        brownCookie = helper.login("brown@test.com", "pass1");
        konCookie   = helper.login("kon@test.com",   "pass2");
    }

    @Test
    @DisplayName("사용자 예약 정상 흐름: 가능 시간 조회 → 예약 → 다시 조회 시 해당 시간이 빠진다")
    void 사용자_예약_정상_흐름() {
        // 1) 미래 날짜 + 테마A의 가능 시간 조회 → 1개
        RestAssured.given().log().all()
                .when().get("/user/themes/" + themeIdA + "/available-times?date=" + FUTURE_DATE)
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1));

        // 2) 10:00으로 예약 생성 (쿠키로 식별)
        Map<String, Object> reservationBody = new HashMap<>();
        reservationBody.put("date", FUTURE_DATE);
        reservationBody.put("timeId", timeId);
        reservationBody.put("themeId", themeIdA);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", brownCookie)
                .contentType(ContentType.JSON)
                .body(reservationBody)
                .when().post("/user/reservations")
                .then().log().all()
                .statusCode(201)
                .body("memberName", is("브라운"))
                .body("date", is(FUTURE_DATE))
                .body("time.id", is(timeId.intValue()))
                .body("theme.id", is(themeIdA.intValue()));

        // 3) 같은 날짜 + 테마A 가능 시간 조회 → 0개
        ExtractableResponse<Response> afterReservation = RestAssured.given().log().all()
                .when().get("/user/themes/" + themeIdA + "/available-times?date=" + FUTURE_DATE)
                .then().log().all()
                .statusCode(200)
                .body("size()", is(0))
                .extract();

        List<Integer> remainingIds = afterReservation.jsonPath().getList("id");
        assert !remainingIds.contains(timeId.intValue());
    }

    @Test
    @DisplayName("같은 날짜+시간이라도 테마가 다르면 각각 예약 가능하다")
    void 같은_시간_다른_테마는_각각_예약_가능() {
        // 테마A에 예약
        Map<String, Object> first = new HashMap<>();
        first.put("date", FUTURE_DATE);
        first.put("timeId", timeId);
        first.put("themeId", themeIdA);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", brownCookie)
                .contentType(ContentType.JSON)
                .body(first)
                .when().post("/user/reservations")
                .then().log().all()
                .statusCode(201);

        // 테마B의 같은 날짜 가능 시간 조회 → 10:00 여전히 보여야 함
        RestAssured.given().log().all()
                .when().get("/user/themes/" + themeIdB + "/available-times?date=" + FUTURE_DATE)
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1));

        // 같은 (날짜, 시간)에 다른 테마로 예약 가능
        Map<String, Object> second = new HashMap<>();
        second.put("date", FUTURE_DATE);
        second.put("timeId", timeId);
        second.put("themeId", themeIdB);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", konCookie)
                .contentType(ContentType.JSON)
                .body(second)
                .when().post("/user/reservations")
                .then().log().all()
                .statusCode(201);
    }
}
