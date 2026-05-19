package roomescape.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.policy.ReservationPolicy;
import roomescape.exception.client.BusinessRuleViolationException;
import roomescape.exception.client.InvalidCommandException;
import roomescape.exception.client.ResourceNotFoundException;
import roomescape.exception.client.UnauthorizedException;
import roomescape.exception.server.DataInconsistencyException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.service.dto.ReservationCreateCommand;
import roomescape.service.dto.ReservationResult;
import roomescape.service.dto.ReservationUpdateCommand;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final MemberRepository memberRepository;
    private final ReservationPolicy reservationPolicy;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            ThemeRepository themeRepository,
            MemberRepository memberRepository,
            ReservationPolicy reservationPolicy
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.memberRepository = memberRepository;
        this.reservationPolicy = reservationPolicy;
    }


    public List<ReservationResult> findAll() {
        return reservationRepository.findAll().stream()
                .map(ReservationResult::from)
                .toList();
    }

    public ReservationResult create(ReservationCreateCommand command) {
        Member member = memberRepository.findById(command.getMemberId())
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다."));
        ReservationTime time = findTimeOrThrow(command.getTimeId());
        Theme theme = findThemeOrThrow(command.getThemeId());

        validateNotDuplicated(command.getDate(), time.getId(), theme.getId());

        Reservation reservation = Reservation.create(
                member,
                command.getDate(),
                time,
                theme,
                reservationPolicy// 정책 객체가 과거 검증을 담당
        );

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResult.from(saved);
    }

    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }

    // 내 예약 조회 - 이름이 아니라 로그인 사용자(memberId) 기준
    public List<ReservationResult> findByMemberId(Long memberId) {
        return reservationRepository.findByMemberIdOrderByDateAscTimeAsc(memberId).stream()
                .map(ReservationResult::from)
                .toList();
    }

    public void deleteByOwner(Long id, Long memberId) {
        Reservation reservation = findByIdAndOwner(id, memberId);
        reservationPolicy.validateCancellable(
                reservation.getDate(),
                reservation.getTime().getStartAt()
        );

        reservationRepository.deleteById(id);
    }

    public ReservationResult updateByOwner(ReservationUpdateCommand command) {
        Reservation reservation = findByIdAndOwner(command.getId(), command.getMemberId());
        reservationPolicy.validateUpdatable(
                reservation.getDate(),
                reservation.getTime().getStartAt()
        );

        ReservationTime newTime = findTimeOrThrow(command.getTimeId());
        reservationPolicy.validateUpdateTarget(command.getDate(), newTime.getStartAt());
        validateNotDuplicatedExcludingSelf(command, reservation.getTheme().getId());

        reservationRepository.updateDateAndTime(command.getId(), command.getDate(), command.getTimeId());
        return ReservationResult.from(findUpdatedReservationOrThrow(command.getId()));
    }

    private ReservationTime findTimeOrThrow(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new InvalidCommandException("존재하지 않는 예약 시간입니다."));
    }

    private Theme findThemeOrThrow(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new InvalidCommandException("존재하지 않는 예약 테마입니다."));
    }

    private Reservation findUpdatedReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new DataInconsistencyException(
                        "저장된 예약을 찾을 수 없습니다. 데이터 정합성 문제가 의심됩니다."
                ));
    }

    // 본인 검증 - 이름 비교가 아니라 검증된 memberId 비교
    private Reservation findByIdAndOwner(Long id, Long memberId) {
        return reservationRepository.findById(id)
                .filter(r -> r.getMember().getId().equals(memberId))
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 예약입니다."));
    }


    private void validateNotDuplicated(LocalDate date, Long timeId, Long themeId) {
        if (reservationRepository.existsByDateAndTimeAndTheme(date, timeId, themeId)) {
            throw new BusinessRuleViolationException(
                    "해당 시간은 이미 예약되었습니다. 다른 시간을 선택해 주세요."
            );
        }
    }

    private void validateNotDuplicatedExcludingSelf(ReservationUpdateCommand command, Long themeId) {
        if (reservationRepository.existsByDateAndTimeAndThemeExcludingId(
                command.getDate(), command.getTimeId(), themeId, command.getId())) {
            throw new BusinessRuleViolationException(
                    "해당 시간은 이미 예약되었습니다. 다른 시간을 선택해 주세요."
            );
        }
    }
}
