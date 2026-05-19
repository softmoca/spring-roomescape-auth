package roomescape.domain;

import java.time.LocalDate;
import roomescape.domain.exception.InvalidDomainException;
import roomescape.domain.policy.ReservationPolicy;

public class Reservation {
    private static final int MAX_NAME_LENGTH = 30;

    private final Long id;
    private final Member member;
    private final LocalDate date;
    private final ReservationTime time;
    private final Theme theme;

    private Reservation(Long id, Member member, LocalDate date, ReservationTime time, Theme theme) {
        validate(member, date, time, theme);
        this.id = id;
        this.member = member;
        this.date = date;
        this.time = time;
        this.theme = theme;
    }

    // 새 예약 생성 (저장 전) - 정책 검증 포함
    public static Reservation create(Member member, LocalDate date,
                                     ReservationTime time, Theme theme,
                                     ReservationPolicy policy) {
        policy.validateCreatable(date, time.getStartAt());
        return new Reservation(null, member, date, time, theme);
    }

    // DB 재구성 (저장 후) - 불변식 검증만
    public static Reservation reconstitute(Long id, Member member, LocalDate date,
                                           ReservationTime time, Theme theme) {
        return new Reservation(id, member, date, time, theme);
    }

    private static void validate(Member member, LocalDate date, ReservationTime time, Theme theme) {
        validateMember(member);
        validateDate(date);
        validateTime(time);
        validateTheme(theme);
    }

    private static void validateMember(Member member) {
        if (member == null) {
            throw new InvalidDomainException("예약자는 비어 있을 수 없습니다.");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new InvalidDomainException("예약 날짜는 비어 있을 수 없습니다.");
        }
    }

    private static void validateTime(ReservationTime time) {
        if (time == null) {
            throw new InvalidDomainException("예약 시간은 비어 있을 수 없습니다.");
        }
    }

    private static void validateTheme(Theme theme) {
        if (theme == null) {
            throw new InvalidDomainException("예약 테마는 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }
    public Member getMember() {
        return member;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReservationTime getTime() {
        return time;
    }

    public Theme getTheme() {
        return theme;
    }
}
