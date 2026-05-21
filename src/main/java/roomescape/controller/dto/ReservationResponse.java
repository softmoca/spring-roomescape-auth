package roomescape.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import roomescape.service.dto.ReservationResult;

public class ReservationResponse {

    private final Long id;
    private final String memberName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate date;

    private final ReservationTimeResponse time;
    private final ThemeResponse theme;

    public ReservationResponse(Long id, String memberName, LocalDate date,
                               ReservationTimeResponse time, ThemeResponse theme) {
        this.id = id;
        this.memberName = memberName;
        this.date = date;
        this.time = time;
        this.theme = theme;
    }

    public static ReservationResponse from(ReservationResult result) {
        return new ReservationResponse(
                result.getId(),
                result.getMemberName(),
                result.getDate(),
                ReservationTimeResponse.from(result.getTime()),
                ThemeResponse.from(result.getTheme())
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

    public ReservationTimeResponse getTime() {
        return time;
    }

    public ThemeResponse getTheme() {
        return theme;
    }
}
