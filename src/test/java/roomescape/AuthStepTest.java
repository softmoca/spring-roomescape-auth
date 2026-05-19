package roomescape;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/*
 * 미션3 1단계 - 인증 요구사항 테스트.
 * 로그인 성공/실패, 비로그인 차단, 로그인 사용자 기반 예약, 본인 검증을 검증한다.
 */
public class AuthStepTest extends IntegrationTest {

    private static final LocalDate FUTURE_DATE = LocalDate.of(2050, 5, 15);

    @Autowired
    private ReservationTestHelper helper;

    private Long timeId;
    private Long themeId;

    @BeforeEach
    void setUp() {
        timeId = helper.insertTime(LocalTime.of(10, 0));
        themeId = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg");
        helper.insertMember("user@a.com", "password", "브라운", "USER");
        helper.insertMember("other@a.com", "password", "콘", "USER");
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("올바른 이메일과 비밀번호로 로그인하면 200을 반환한다")
        void 로그인_성공() {
            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(Map.of("email", "user@a.com", "password", "password"))
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(200)
                    .cookie("JSESSIONID");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401을 반환한다")
        void 비밀번호_불일치() {
            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(Map.of("email", "user@a.com", "password", "wrong"))
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 401을 반환한다")
        void 존재하지_않는_이메일() {
            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(Map.of("email", "nobody@a.com", "password", "password"))
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("이메일이 비어 있으면 400을 반환한다")
        void 이메일_누락() {
            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(Map.of("password", "password"))
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(400)
                    .body("message", is("이메일을 입력해 주세요."));
        }
    }

    @Nested
    @DisplayName("인증이 필요한 요청")
    class AuthRequired {

        @Test
        @DisplayName("로그인하지 않고 내 예약을 조회하면 401을 반환한다")
        void 비로그인_조회_차단() {
            RestAssured.given().log().all()
                    .when().get("/user/reservations")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("로그인이 필요합니다."));
        }

        @Test
        @DisplayName("로그인하지 않고 예약을 생성하면 401을 반환한다")
        void 비로그인_생성_차단() {
            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", timeId);
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
        @DisplayName("로그아웃 후 같은 세션으로 요청하면 401을 반환한다")
        void 로그아웃_후_차단() {
            String cookie = helper.loginAndGetCookie("user@a.com", "password");

            // 로그아웃
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .when().post("/logout")
                    .then().log().all()
                    .statusCode(200);

            // 무효화된 세션으로 다시 요청 → 401
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .when().get("/user/reservations")
                    .then().log().all()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("로그인 사용자 기반 예약")
    class LoginBasedReservation {

        @Test
        @DisplayName("로그인한 사용자로 예약하면 예약자가 로그인 사용자로 기록된다")
        void 로그인_사용자로_예약() {
            String cookie = helper.loginAndGetCookie("user@a.com", "password");

            Map<String, Object> body = new HashMap<>();
            body.put("date", FUTURE_DATE.toString());
            body.put("timeId", timeId);
            body.put("themeId", themeId);

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/user/reservations")
                    .then().log().all()
                    .statusCode(201)
                    .body("member.name", is("브라운"));
        }

        @Test
        @DisplayName("다른 사람의 예약을 취소하려 하면 404를 반환한다")
        void 다른_사람의_예약_취소_차단() {
            Long brownId = helper.insertMember("brown2@a.com", "password", "브라운2", "USER");
            Long reservationId = helper.insertReservationAndReturnId(brownId, FUTURE_DATE, timeId, themeId);

            // 콘으로 로그인해서 브라운2의 예약 취소 시도
            String konCookie = helper.loginAndGetCookie("other@a.com", "password");

            RestAssured.given().log().all()
                    .cookie("JSESSIONID", konCookie)
                    .when().delete("/user/reservations/" + reservationId)
                    .then().log().all()
                    .statusCode(404)
                    .body("message", is("존재하지 않는 예약입니다."));
        }
    }
}
