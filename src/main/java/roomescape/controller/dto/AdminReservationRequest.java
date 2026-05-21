package roomescape.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AdminReservationRequest {

    @NotNull(message = "회원 ID는 비어 있을 수 없습니다.")
    private Long memberId;

    @NotNull(message = "예약 날짜는 비어 있을 수 없습니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "예약 시간을 선택해 주세요.")
    private Long timeId;

    @NotNull(message = "예약 테마를 선택해 주세요.")
    private Long themeId;

    public AdminReservationRequest() {
    }

    public Long getMemberId() {
        return memberId;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getTimeId() {
        return timeId;
    }

    public Long getThemeId() {
        return themeId;
    }
}
