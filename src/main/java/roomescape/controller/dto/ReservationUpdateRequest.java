package roomescape.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import roomescape.service.dto.ReservationUpdateCommand;

public class ReservationUpdateRequest {
    @NotNull(message = "예약 날짜는 비어 있을 수 없습니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "예약 시간을 선택해 주세요.")
    private Long timeId;

    public ReservationUpdateRequest() {
    }

    public ReservationUpdateCommand toCommand(Long id, Long memberId) {
        return new ReservationUpdateCommand(id, memberId, date, timeId);
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getTimeId() {
        return timeId;
    }
}
