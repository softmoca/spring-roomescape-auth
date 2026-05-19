package roomescape.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import roomescape.service.dto.ReservationCreateCommand;

public class ReservationRequest {

    @NotNull(message = "예약 날짜는 비어 있을 수 없습니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "예약 시간을 선택해 주세요.")
    private Long timeId;

    @NotNull(message = "예약 테마를 선택해 주세요.")
    private Long themeId;

    public ReservationRequest() {
    }
    // 예약자(memberId)는 요청 본문이 아니라 로그인 세션에서 온다
    public ReservationCreateCommand toCommand(Long memberId) {
        return new ReservationCreateCommand(memberId, date, timeId, themeId);
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
