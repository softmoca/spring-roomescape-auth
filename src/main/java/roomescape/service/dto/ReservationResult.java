package roomescape.service.dto;

import java.time.LocalDate;
import roomescape.domain.Reservation;

public class ReservationResult {

    private final Long id;
    private final String memberName;
    private final LocalDate date;
    private final ReservationTimeResult time;
    private final ThemeResult theme;

    public ReservationResult(
            Long id,
            String memberName,
            LocalDate date,
            ReservationTimeResult time,
            ThemeResult theme
    ) {
        this.id = id;
        this.memberName = memberName;
        this.date = date;
        this.time = time;
        this.theme = theme;
    }

    public static ReservationResult from(Reservation reservation) {
        return new ReservationResult(
                reservation.getId(),
                reservation.getMember().getName(),
                reservation.getDate(),
                ReservationTimeResult.from(reservation.getTime()),
                ThemeResult.from(reservation.getTheme())
        );
    }

    public Long getId() {
        return id;
    }

    public String getMemberName() {
        return memberName;
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
