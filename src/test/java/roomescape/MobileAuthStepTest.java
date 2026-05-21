package roomescape;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import roomescape.support.ReservationTestHelper;
/**
 * 2단계 모바일 인증 시나리오 통합 테스트.
 *
 * 흐름:
 *   1. POST /api/login  → token 발급
 *   2. GET  /user/reservations (Authorization: Bearer <token>) → 200
 *   3. 토큰 없이 인증 필요 API 호출 → 401
 *   4. 위조된 토큰으로 호출 → 401
 *   5. 웹 세션 쿠키와 모바일 토큰 공존 확인

 */



class MobileAuthStepTest extends IntegrationTest {

    @Autowired
    private ReservationTestHelper helper;

    /* ───────────────────────────────────────────────
     * 1. 모바일 로그인
     * ─────────────────────────────────────────────── */
    @Test
    @DisplayName("모바일 로그인 성공 — token과 name을 응답 본문에 반환한다")
    void 모바일_로그인_성공() {
        helper.insertMember("brown@email.com", "password", "브라운");

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "brown@email.com", "password", "password"))
                .when().post("/api/login")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("token", org.hamcrest.Matchers.notNullValue())
                .body("name", org.hamcrest.Matchers.is("브라운"));
    }

    @Test
    @DisplayName("모바일 로그인 실패 — 잘못된 비밀번호는 401")
    void 모바일_로그인_실패_잘못된_비밀번호() {
        helper.insertMember("brown@email.com", "password", "브라운");

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "brown@email.com", "password", "wrong"))
                .when().post("/api/login")
                .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("모바일 로그인 실패 — 존재하지 않는 이메일은 401")
    void 모바일_로그인_실패_없는_이메일() {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "nobody@email.com", "password", "password"))
                .when().post("/api/login")
                .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    /* ───────────────────────────────────────────────
     * 2. 토큰으로 인증 필요 API 호출
     * ─────────────────────────────────────────────── */
    @Test
    @DisplayName("Authorization 헤더(Bearer 토큰)로 내 예약 목록 조회 — 200")
    void 토큰으로_내_예약_조회() {
        helper.insertMember("brown@email.com", "password", "브라운");
        String token = mobileLogin("brown@email.com", "password");

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + token)
                .when().get("/user/reservations")   // /mine 없음 — GET /user/reservations가 내 예약 목록
                .then().log().all()
                .statusCode(HttpStatus.OK.value());
    }

    /* ───────────────────────────────────────────────
     * 3. 인증 실패 케이스
     * ─────────────────────────────────────────────── */
    @Test
    @DisplayName("Authorization 헤더 없이 인증 필요 API 호출 — 401")
    void 토큰_없이_인증_필요_API_호출() {
        RestAssured.given().log().all()
                .when().get("/user/reservations")
                .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("위조된 토큰으로 인증 필요 API 호출 — 401")
    void 위조된_토큰으로_호출() {
        RestAssured.given().log().all()
                .header("Authorization", "Bearer this.is.not.a.valid.token")
                .when().get("/user/reservations")
                .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Bearer 접두사 없이 헤더만 있으면 — 세션도 없으면 401")
    void Bearer_접두사_없이_호출() {
        helper.insertMember("brown@email.com", "password", "브라운");
        String token = mobileLogin("brown@email.com", "password");

        RestAssured.given().log().all()
                .header("Authorization", token)   // Bearer 없음
                .when().get("/user/reservations")
                .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    /* ───────────────────────────────────────────────
     * 4. 웹 세션과 모바일 토큰 공존 확인
     * ─────────────────────────────────────────────── */
    @Test
    @DisplayName("웹 세션 쿠키와 모바일 토큰이 같은 API에서 각각 동작한다")
    void 세션과_토큰_공존() {
        helper.insertMember("brown@email.com", "password", "브라운");

        // 웹: 쿠키 기반
        String sessionCookie = helper.login("brown@email.com", "password");
        RestAssured.given()
                .cookie("JSESSIONID", sessionCookie)
                .when().get("/user/reservations")
                .then().statusCode(HttpStatus.OK.value());

        // 모바일: 토큰 기반
        String token = mobileLogin("brown@email.com", "password");
        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/user/reservations")
                .then().statusCode(HttpStatus.OK.value());
    }

    // ──────── 헬퍼 ────────

    /** POST /api/login → token 문자열 반환 */
    private String mobileLogin(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post("/api/login")
                .then().statusCode(HttpStatus.OK.value())
                .extract().jsonPath().getString("token");
    }
}
