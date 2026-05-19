package roomescape;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/*
 * 미션1 - 관리자 예약 관리 API 테스트.
 * 미션3: 관리자 예약 생성도 @LoginMember를 통해 인증된 사용자 정보를 받는다.
 */
public class MissionStepTest extends IntegrationTest {

    private static final String FUTURE_DATE = "2050-10-10";

    @Autowired
    private ReservationTestHelper helper;

    private Long timeId;
    private Long themeId;
    private String adminCookie;

    @BeforeEach
    void setUp() {
        timeId = helper.insertTime(LocalTime.of(10, 0));
        themeId = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg");
        helper.insertMember("admin@a.com", "password", "관리자", "ADMIN");
        adminCookie = helper.loginAndGetCookie("admin@a.com", "password");
    }

    @Test
    @DisplayName("관리자가 예약을 생성하면 201 + 생성된 예약을 반환한다")
    void 관리자_예약_생성() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", adminCookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/admin/reservations")
                .then().log().all()
                .statusCode(201)
                .body("id", is(1))
                .body("date", is(FUTURE_DATE));
    }

    @Test
    @DisplayName("전체 예약 목록을 조회하면 200 + 목록을 반환한다")
    void 예약_목록_조회() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        RestAssured.given()
                .cookie("JSESSIONID", adminCookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/admin/reservations")
                .then().statusCode(201);

        RestAssured.given().log().all()
                .cookie("JSESSIONID", adminCookie)
                .when().get("/admin/reservations")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @DisplayName("예약을 삭제하면 204를 반환하고 목록에서 사라진다")
    void 예약_삭제() {
        Map<String, Object> body = new HashMap<>();
        body.put("date", FUTURE_DATE);
        body.put("timeId", timeId);
        body.put("themeId", themeId);

        Long reservationId = RestAssured.given()
                .cookie("JSESSIONID", adminCookie)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/admin/reservations")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        RestAssured.given().log().all()
                .cookie("JSESSIONID", adminCookie)
                .when().delete("/admin/reservations/" + reservationId)
                .then().log().all()
                .statusCode(204);

        RestAssured.given()
                .cookie("JSESSIONID", adminCookie)
                .when().get("/admin/reservations")
                .then().statusCode(200)
                .body("size()", is(0));
    }
}
