package roomescape.service.dto;

import java.time.LocalDate;
import roomescape.domain.Reservation;

public class ReservationResult {

    private final Long id;
    private final MemberResult member;
    private final LocalDate date;
    private final ReservationTimeResult time;
    private final ThemeResult theme;

    public ReservationResult(
            Long id,
            MemberResult member,
            LocalDate date,
            ReservationTimeResult time,
            ThemeResult theme
    ) {
        this.id = id;
        this.member=member;
        this.date = date;
        this.time = time;
        this.theme = theme;
    }

    public static ReservationResult from(Reservation reservation) {
        return new ReservationResult(
                reservation.getId(),
                MemberResult.from(reservation.getMember()),
                reservation.getDate(),
                ReservationTimeResult.from(reservation.getTime()),
                ThemeResult.from(reservation.getTheme())
        );
    }

    public Long getId() {
        return id;
    }

    public MemberResult getMember() {
        return member;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReservationTimeResult getTime() {
        return time;
    }

    public ThemeResult getTheme() {
        return theme;
    }
}
