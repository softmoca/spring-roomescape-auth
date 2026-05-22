package roomescape;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/**
 * 3단계 — 매니저 인가 시나리오 통합 테스트.
 *
 * 검증 대상:
 *   - 매니저 로그인 → 자기 매장 예약 조회 (200)
 *   - 매니저 로그인 → 자기 매장 예약 삭제 (204)
 *   - 매니저 로그인 → 다른 매장 예약 삭제 → 403
 *   - 비로그인 → /admin/** 접근 → 401
 *   - 일반 사용자(매니저 아님) → /admin/** 접근 → 403
 *
 * 정책 (3.4 C안):
 *   - 매니저 영역: 존재하는 예약에 권한 없으면 403 (존재 사실을 숨기지 않음)
 *   - 사용자 영역: 다른 사람 예약이면 404 (존재 여부 노출 방지)
 */
@DisplayName("매니저 인가 시나리오")
class ManagerReservationStepTest extends IntegrationTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(7);

    @Autowired
    private ReservationTestHelper helper;

    // 강남점 매니저
    private Long gangnamManagerMemberId;
    private String gangnamManagerCookie;

    // 홍대점 매니저
    private Long hongdaeManagerMemberId;
    private String hongdaeManagerCookie;

    // 일반 사용자 (매니저 아님)
    private Long normalUserId;
    private String normalUserCookie;

    // 강남점 예약
    private Long gangnamReservationId;

    // 홍대점 예약
    private Long hongdaeReservationId;

    @BeforeEach
    void setUp() {
        // 매장 2개
        Long gangnamStoreId  = helper.insertStore("강남점");
        Long hongdaeStoreId  = helper.insertStore("홍대점");

        // 테마 (매장별)
        Long gangnamThemeId  = helper.insertTheme("무인도 탈출", gangnamStoreId);
        Long hongdaeThemeId  = helper.insertTheme("도시 탈출",   hongdaeStoreId);

        // 시간
        Long timeId = helper.insertTime(LocalTime.of(14, 0));

        // 강남점 매니저 세팅
        gangnamManagerMemberId = helper.insertMember("gangnam@mgr.com", "pass", "강남매니저");
        helper.insertManager(gangnamManagerMemberId, gangnamStoreId);
        gangnamManagerCookie = helper.login("gangnam@mgr.com", "pass");

        // 홍대점 매니저 세팅
        hongdaeManagerMemberId = helper.insertMember("hongdae@mgr.com", "pass", "홍대매니저");
        helper.insertManager(hongdaeManagerMemberId, hongdaeStoreId);
        hongdaeManagerCookie = helper.login("hongdae@mgr.com", "pass");

        // 일반 사용자 세팅 (매니저 row 없음)
        normalUserId = helper.insertMember("user@test.com", "pass", "일반사용자");
        normalUserCookie = helper.login("user@test.com", "pass");

        // 예약: 강남점 예약 1건, 홍대점 예약 1건
        gangnamReservationId = helper.insertReservationAndReturnId(
                normalUserId, FUTURE_DATE, timeId, gangnamThemeId);
        hongdaeReservationId = helper.insertReservationAndReturnId(
                normalUserId, FUTURE_DATE, timeId, hongdaeThemeId);
    }

    // ──────── 비로그인 / 역할 없음 ────────

    @Nested
    @DisplayName("접근 제어")
    class AccessControl {

        @Test
        @DisplayName("비로그인 → /admin/reservations 접근 → 401")
        void 비로그인_접근_401() {
            RestAssured.given().log().all()
                    .when().get("/admin/reservations")
                    .then().log().all()
                    .statusCode(401);
        }

        @Test
        @DisplayName("일반 사용자(매니저 아님) → /admin/reservations 접근 → 403")
        void 일반사용자_접근_403() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", normalUserCookie)
                    .when().get("/admin/reservations")
                    .then().log().all()
                    .statusCode(403);
        }

        @Test
        @DisplayName("비로그인 → DELETE /admin/reservations/{id} → 401")
        void 비로그인_삭제_401() {
            RestAssured.given().log().all()
                    .when().delete("/admin/reservations/" + gangnamReservationId)
                    .then().log().all()
                    .statusCode(401);
        }

        @Test
        @DisplayName("일반 사용자 → DELETE /admin/reservations/{id} → 403")
        void 일반사용자_삭제_403() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", normalUserCookie)
                    .when().delete("/admin/reservations/" + gangnamReservationId)
                    .then().log().all()
                    .statusCode(403);
        }
    }

    // ──────── 자기 매장 조회 ────────

    @Nested
    @DisplayName("자기 매장 예약 조회")
    class MyStoreReservations {

        @Test
        @DisplayName("강남점 매니저 → 강남점 예약만 반환된다")
        void 강남점_매니저_강남점_예약만_조회() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", gangnamManagerCookie)
                    .when().get("/admin/reservations")
                    .then().log().all()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].id", is(gangnamReservationId.intValue()));
        }

        @Test
        @DisplayName("홍대점 매니저 → 홍대점 예약만 반환된다")
        void 홍대점_매니저_홍대점_예약만_조회() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", hongdaeManagerCookie)
                    .when().get("/admin/reservations")
                    .then().log().all()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].id", is(hongdaeReservationId.intValue()));
        }
    }

    // ──────── 자기 매장 삭제 ────────

    @Nested
    @DisplayName("매장 예약 삭제 인가")
    class ManagerDelete {

        @Test
        @DisplayName("강남점 매니저 → 강남점 예약 삭제 → 204")
        void 자기_매장_예약_삭제_성공() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", gangnamManagerCookie)
                    .when().delete("/admin/reservations/" + gangnamReservationId)
                    .then().log().all()
                    .statusCode(204);
        }

        @Test
        @DisplayName("강남점 매니저 → 홍대점 예약 삭제 시도 → 403")
        void 다른_매장_예약_삭제_403() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", gangnamManagerCookie)
                    .when().delete("/admin/reservations/" + hongdaeReservationId)
                    .then().log().all()
                    .statusCode(403)
                    .body("message", is("다른 매장의 예약에는 접근할 수 없습니다."));
        }

        @Test
        @DisplayName("강남점 매니저 → 존재하지 않는 예약 삭제 → 404")
        void 존재하지_않는_예약_삭제_404() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", gangnamManagerCookie)
                    .when().delete("/admin/reservations/9999")
                    .then().log().all()
                    .statusCode(404);
        }

        @Test
        @DisplayName("홍대점 매니저 → 강남점 예약 삭제 시도 → 403")
        void 홍대점_매니저_강남점_예약_삭제_403() {
            RestAssured.given().log().all()
                    .cookie("JSESSIONID", hongdaeManagerCookie)
                    .when().delete("/admin/reservations/" + gangnamReservationId)
                    .then().log().all()
                    .statusCode(403)
                    .body("message", is("다른 매장의 예약에는 접근할 수 없습니다."));
        }
    }

    // ──────── 인증·인가 구분 명확성 ────────

    @Nested
    @DisplayName("인증 실패와 인가 실패의 구분")
    class AuthVsAuthz {

        @Test
        @DisplayName("비로그인은 401, 로그인했지만 매니저 아니면 403 — 두 응답이 구분된다")
        void 인증실패_인가실패_구분() {
            // 401
            RestAssured.given()
                    .when().delete("/admin/reservations/" + gangnamReservationId)
                    .then().statusCode(401);

            // 403
            RestAssured.given()
                    .cookie("JSESSIONID", normalUserCookie)
                    .when().delete("/admin/reservations/" + gangnamReservationId)
                    .then().statusCode(403);
        }
    }
}
