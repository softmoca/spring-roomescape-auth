package roomescape.service.dto;

import java.time.LocalDate;
import roomescape.exception.client.InvalidCommandException;

public class ReservationUpdateCommand {
    private final Long id;
    private final Long memberId;
    private final LocalDate date;
    private final Long timeId;

    public ReservationUpdateCommand(Long id, Long memberId, LocalDate date, Long timeId) {
        validate(id, memberId, date, timeId);
        this.id = id;
        this.memberId = memberId;
        this.date = date;
        this.timeId = timeId;
    }

    private void validate(Long id, Long memberId, LocalDate date, Long timeId) {
        if (id == null) {
            throw new InvalidCommandException("예약 ID는 비어 있을 수 없습니다.");
        }
        if (memberId == null) {
            throw new InvalidCommandException("예약자ID는 비어 있을 수 없습니다.");
        }
        if (date == null) {
            throw new InvalidCommandException("예약 날짜는 비어 있을 수 없습니다.");
        }
        if (timeId == null) {
            throw new InvalidCommandException("예약 시간을 선택해 주세요.");
        }
    }


    public Long getId() {
        return id;
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
}
