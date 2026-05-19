package roomescape;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.support.ReservationTestHelper;

/*
 * 인기 테마 API 테스트.
 * 최근 일주일간 예약이 많은 테마를 예약 건수 내림차순으로 최대 10개 반환한다.
 * 집계 기준은 "테마별 예약 건수"이므로 예약자가 누구인지는 무관 → 회원 1명 재사용.
 * /user/themes/popular 는 인증 불필요 → 쿠키 없이 호출.
 */
public class PopularThemeStepTest extends IntegrationTest {

    @Autowired
    private ReservationTestHelper helper;

    private Long memberId;
    private Long timeId;

    @BeforeEach
    void setUp() {
        timeId = helper.insertTime(LocalTime.of(10, 0));
        // 예약자는 집계에 영향을 주지 않으므로 회원 한 명으로 모든 예약을 만든다
        memberId = helper.insertMember("user@a.com", "password", "브라운", "USER");
    }

    @Test
    @DisplayName("최근 일주일 예약 건수 내림차순으로 인기 테마를 반환한다")
    void 인기_테마_정렬() {
        LocalDate base = LocalDate.now().minusDays(1);

        Long themeA = helper.insertTheme("테마A", "설명A", "https://example.com/a.jpg");
        Long themeB = helper.insertTheme("테마B", "설명B", "https://example.com/b.jpg");
        Long themeC = helper.insertTheme("테마C", "설명C", "https://example.com/c.jpg");

        // 테마A: 3건, 테마B: 2건, 테마C: 1건
        // 같은 (member, date, time, theme) 중복을 피하려고 날짜를 하루씩 다르게 둔다
        helper.insertReservation(memberId, base, timeId, themeA);
        helper.insertReservation(memberId, base.minusDays(1), timeId, themeA);
        helper.insertReservation(memberId, base.minusDays(2), timeId, themeA);
        helper.insertReservation(memberId, base, timeId, themeB);
        helper.insertReservation(memberId, base.minusDays(1), timeId, themeB);
        helper.insertReservation(memberId, base, timeId, themeC);

        RestAssured.given().log().all()
                .when().get("/user/themes/popular")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(3))
                .body("[0].name", is("테마A"))
                .body("[1].name", is("테마B"))
                .body("[2].name", is("테마C"));
    }

    @Test
    @DisplayName("인기 테마는 최대 10개까지만 반환한다")
    void 인기_테마_최대_10개() {
        LocalDate base = LocalDate.now().minusDays(1);

        // 테마 12개를 만들고 각각 예약 1건씩 → 결과는 10개로 잘려야 함
        for (int i = 1; i <= 12; i++) {
            Long themeId = helper.insertTheme(
                    "테마" + i, "설명" + i, "https://example.com/" + i + ".jpg");
            helper.insertReservation(memberId, base.minusDays(i % 7), timeId, themeId);
        }

        RestAssured.given().log().all()
                .when().get("/user/themes/popular")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(10));
    }

    @Test
    @DisplayName("최근 일주일을 벗어난 예약은 인기 집계에서 제외된다")
    void 기간_밖_예약_제외() {
        LocalDate longAgo = LocalDate.now().minusDays(30);

        Long themeOld = helper.insertTheme("오래된테마", "설명", "https://example.com/old.jpg");
        helper.insertReservation(memberId, longAgo, timeId, themeOld);

        RestAssured.given().log().all()
                .when().get("/user/themes/popular")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(0));
    }
}
