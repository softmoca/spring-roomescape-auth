package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.domain.exception.InvalidDomainException;
import roomescape.domain.policy.ReservationPolicy;
import roomescape.support.AlwaysAllowPolicy;

class ReservationTest {

    private static final Member VALID_MEMBER =
            Member.reconstitute(1L, "user@a.com", "password", "브라운", MemberRole.USER);

    private static final ReservationTime VALID_TIME =
            ReservationTime.reconstitute(1L, LocalTime.of(10, 0));
    private static final LocalDate VALID_DATE = LocalDate.of(2026, 1, 1);
    private static final Theme VALID_THEME =
            Theme.reconstitute(1L, "무인도 탈출",
                    "갯벌이 많은 무인도를 탈출하는 흥미진진 대탈출!",
                    "https://picsum.photos/seed/roomescape1/800/600.jpg");

    private static final ReservationPolicy ALLOW_ALL = new AlwaysAllowPolicy();


    @Test
    @DisplayName("유효한 값으로 예약을 생성할 수 있다")
    void 정상_생성() {
        assertThatCode(() -> Reservation.create(
                VALID_MEMBER, LocalDate.now().plusDays(1), VALID_TIME, VALID_THEME, ALLOW_ALL))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DB에서 재구성할 수 있다")
    void DB에서_재구성할_수_있다() {
        assertDoesNotThrow(() ->
                Reservation.reconstitute(1L, VALID_MEMBER, VALID_DATE, VALID_TIME, VALID_THEME));
    }


    @Test
    @DisplayName("예약자(member)가 null이면 예외가 발생한다")
    void 예약자_null_예외() {
        InvalidDomainException exception = assertThrows(
                InvalidDomainException.class,
                () -> Reservation.create(
                        null, LocalDate.now().plusDays(1), VALID_TIME, VALID_THEME, ALLOW_ALL)
        );
        assertEquals("예약자는 비어 있을 수 없습니다.", exception.getMessage());
    }


    @Test
    @DisplayName("날짜가 null이면 예외가 발생한다")
    void 날짜가_null이면_예외가_발생한다() {
        InvalidDomainException exception = assertThrows(
                InvalidDomainException.class,
                () -> Reservation.reconstitute(1L, VALID_MEMBER, null, VALID_TIME, VALID_THEME)
        );
        assertEquals("예약 날짜는 비어 있을 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("시간이 null이면 예외가 발생한다")
    void 시간이_null이면_예외가_발생한다() {
        InvalidDomainException exception = assertThrows(
                InvalidDomainException.class,
                () -> Reservation.reconstitute(1L, VALID_MEMBER, VALID_DATE, null, VALID_THEME)
        );
        assertEquals("예약 시간은 비어 있을 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("테마가 null이면 예외가 발생한다")
    void 테마가_null이면_예외가_발생한다() {
        InvalidDomainException exception = assertThrows(
                InvalidDomainException.class,
                () -> Reservation.reconstitute(1L, VALID_MEMBER, VALID_DATE, VALID_TIME, null)
        );
        assertEquals("예약 테마는 비어 있을 수 없습니다.", exception.getMessage());
    }


}
