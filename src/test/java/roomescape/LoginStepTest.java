package roomescape;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/*
 * 1단계 — 로그인/로그아웃 API 통합 테스트.
 */
public class LoginStepTest extends IntegrationTest {

    @Autowired
    private ReservationTestHelper helper;

    @BeforeEach
    void setUp() {
        helper.insertMember("brown@test.com", "password1", "브라운");
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("올바른 이메일/비밀번호로 로그인하면 200 + 이름 반환")
        void 로그인_성공() {
            Map<String, String> body = new HashMap<>();
            body.put("email", "brown@test.com");
            body.put("password", "password1");

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(200)
                    .body("name", is("브라운"))
                    .cookie("JSESSIONID", notNullValue());
        }

        @Test
        @DisplayName("잘못된 비밀번호 → 401")
        void 잘못된_비밀번호() {
            Map<String, String> body = new HashMap<>();
            body.put("email", "brown@test.com");
            body.put("password", "wrong");

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → 401")
        void 존재하지_않는_이메일() {
            Map<String, String> body = new HashMap<>();
            body.put("email", "nobody@test.com");
            body.put("password", "password1");

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("이메일 누락 → 400")
        void 이메일_누락() {
            Map<String, String> body = new HashMap<>();
            body.put("password", "password1");

            RestAssured.given().log().all()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/login")
                    .then().log().all()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그인 후 로그아웃하면 204, 이후 인증 필요 API 접근 시 401")
        void 로그아웃_후_접근_거부() {
            // 로그인
            String cookie = helper.login("brown@test.com", "password1");

            // 로그아웃
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .when().post("/logout")
                    .then().log().all()
                    .statusCode(204);

            // 로그아웃 후 인증 필요 API 접근 → 401
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", cookie)
                    .when().get("/user/reservations")
                    .then().log().all()
                    .statusCode(401)
                    .body("message", is("로그인이 필요합니다."));
        }
    }
}
