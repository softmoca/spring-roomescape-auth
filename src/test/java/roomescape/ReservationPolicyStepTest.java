package roomescape;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.exception.client.BusinessRuleViolationException;
import roomescape.service.ReservationService;
import roomescape.service.dto.ReservationCreateCommand;
import roomescape.support.ReservationTestHelper;

/*
 * 예약 정책 테스트 - Service를 직접 호출하는 통합 테스트.
 * HTTP 계층을 거치지 않으므로 세션/쿠키 개념이 없다.
 * 미션3: ReservationCreateCommand가 예약자 이름 대신 memberId를 받는다.
 */
public class ReservationPolicyStepTest extends IntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationTestHelper helper;

    private Long memberId;
    private Long timeId;
    private Long themeId;

    @BeforeEach
    void setUp() {
        memberId = helper.insertMember("user@a.com", "password", "브라운", "USER");
        timeId = helper.insertTime(LocalTime.of(10, 0));
        themeId = helper.insertTheme("테마A", "설명", "https://example.com/a.jpg");
    }

    @Test
    @DisplayName("과거 날짜로 예약을 시도하면 예외가 발생한다")
    void 과거_날짜_예약_불가() {
        ReservationCreateCommand command = new ReservationCreateCommand(
                memberId, LocalDate.now().minusDays(1), timeId, themeId);

        assertThatThrownBy(() -> reservationService.create(command))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("같은 날짜, 시간, 테마에 중복 예약을 시도하면 예외가 발생한다")
    void 중복_예약_불가() {
        LocalDate future = LocalDate.now().plusDays(10);
        ReservationCreateCommand command = new ReservationCreateCommand(
                memberId, future, timeId, themeId);
        reservationService.create(command);

        assertThatThrownBy(() -> reservationService.create(command))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("미래 날짜로 정상 예약하면 예외가 발생하지 않는다")
    void 미래_날짜_예약_가능() {
        ReservationCreateCommand command = new ReservationCreateCommand(
                memberId, LocalDate.now().plusDays(10), timeId, themeId);

        assertThatCode(() -> reservationService.create(command))
                .doesNotThrowAnyException();
    }
}
