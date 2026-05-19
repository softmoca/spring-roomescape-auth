package roomescape;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/*
 * 에러 응답 형식 테스트.
 * 모든 예외는 GlobalExceptionHandler를 거쳐 {message} 단일 필드로 응답된다.
 * 스택트레이스/DB 메시지가 노출되지 않아야 한다.
 */
public class ErrorResponseStepTest extends IntegrationTest {

    private static final String FUTURE_DATE = "2050-09-09";

    @Autowired
    private ReservationTestHelper helper;

    private Long timeId;
    private Long themeId;
    private Long memberId;
    private String cookie;

    @BeforeEach
    void setUp() {
        timeId = helper.insertTime(LocalTime.of(10, 0));
        themeId = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg");
        memberId = helper.insertMember("user@a.com", "password", "브라운", "USER");
        cookie = helper.loginAndGetCookie("user@a.com", "password");
    }

    @Test
    @DisplayName("존재하지 않는 timeId로 예약 시 400 + 에러 메시지를 반환한다")
    void 존재하지_않는_시간() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", 9999);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", cookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/user/reservations")
                .then().log().all()
                .statusCode(400)
                .body("message", is("존재하지 않는 예약 시간입니다."))
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("중복 예약 시 400 + 에러 메시지를 반환한다")
    void 중복_예약() {
        helper.insertReservation(memberId, LocalDate.parse(FUTURE_DATE), timeId, themeId);

        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", timeId);
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
    @DisplayName("과거 날짜로 예약 시 400 + 에러 메시지를 반환한다")
    void 과거_날짜_예약() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", LocalDate.now().minusDays(1).toString());
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", cookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/user/reservations")
                .then().log().all()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("필수 필드(date) 누락 시 400 + 에러 메시지를 반환한다")
    void 필수_필드_누락() {
        Map<String, Object> body = new HashMap<>();
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", cookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/user/reservations")
                .then().log().all()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("에러 응답에 스택트레이스나 DB 메시지가 노출되지 않는다")
    void 내부_정보_비노출() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", 9999);
        body.put("themeId", themeId);

        String responseBody = RestAssured.given()
                .cookie("JSESSIONID", cookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/user/reservations")
                .then().statusCode(400)
                .extract().asString();

        // 응답 본문에 내부 구현 흔적이 없어야 한다
        assert !responseBody.contains("Exception");
        assert !responseBody.contains("SQL");
        assert !responseBody.contains("at roomescape");
    }
}
