package roomescape.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import roomescape.service.dto.ReservationResult;

public class ReservationResponse {
    private final Long id;
    private final MemberResponse member;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate date;

    private final ReservationTimeResponse time;
    private final ThemeResponse theme;

    public ReservationResponse(Long id, MemberResponse member, LocalDate date, ReservationTimeResponse time,
                               ThemeResponse theme) {
        this.id = id;
        this.member=member;
        this.date = date;
        this.time = time;
        this.theme = theme;
    }

    public static ReservationResponse from(ReservationResult result) {
        return new ReservationResponse(
                result.getId(),
                MemberResponse.from(result.getMember()),
                result.getDate(),
                ReservationTimeResponse.from(result.getTime()),
                ThemeResponse.from(result.getTheme())
        );
    }

    public Long getId() {
        return id;
    }

    public MemberResponse getMember() {
        return member;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReservationTimeResponse getTime() {
        return time;
    }

    public ThemeResponse getTheme() {
        return theme;
    }
}
