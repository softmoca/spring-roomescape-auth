package roomescape.service.dto;

import java.time.LocalDate;
import roomescape.exception.client.InvalidCommandException;

public class ReservationCreateCommand {

    private final Long memberId;
    private final LocalDate date;
    private final Long timeId;
    private final Long themeId;

    public ReservationCreateCommand(Long memberId, LocalDate date, Long timeId, Long themeId) {
        validate(memberId, date, timeId, themeId);
        this.memberId = memberId;
        this.date = date;
        this.timeId = timeId;
        this.themeId = themeId;
    }

    private void validate(Long memberId, LocalDate date, Long timeId, Long themeId) {
        if (memberId == null) {
            throw new InvalidCommandException("회원 정보는 비어 있을 수 없습니다.");
        }
        if (date == null) {
            throw new InvalidCommandException("예약 날짜는 비어 있을 수 없습니다.");
        }
        if (timeId == null) {
            throw new InvalidCommandException("예약 시간을 선택해 주세요.");
        }
        if (themeId == null) {
            throw new InvalidCommandException("예약 테마를 선택해 주세요.");
        }
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
