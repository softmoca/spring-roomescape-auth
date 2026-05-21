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
import roomescape.exception.client.ResourceNotFoundException;
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
        Member member = findMemberOrThrow(command.getMemberId());
        ReservationTime time = findTimeOrThrow(command.getTimeId());
        Theme theme = findThemeOrThrow(command.getThemeId());

        validateNotDuplicated(command.getDate(), time.getId(), theme.getId());

        Reservation reservation = Reservation.create(member, command.getDate(), time, theme, reservationPolicy);
        Reservation saved = reservationRepository.save(reservation);
        return ReservationResult.from(saved);
    }

    /** 어드민 전용 삭제 — 소유자 검증 없음 */
    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }

    /** 사용자 본인 예약 조회 */
    public List<ReservationResult> findByMember(Long memberId) {
        return reservationRepository.findByMemberIdOrderByDateAscTimeAsc(memberId).stream()
                .map(ReservationResult::from)
                .toList();
    }

    /** 사용자 본인 예약 취소 */
    public void deleteByOwner(Long reservationId, Long memberId) {
        Reservation reservation = findByIdAndMember(reservationId, memberId);
        reservationPolicy.validateCancellable(
                reservation.getDate(),
                reservation.getTime().getStartAt()
        );
        reservationRepository.deleteById(reservationId);
    }

    /** 사용자 본인 예약 변경 */
    public ReservationResult updateByOwner(ReservationUpdateCommand command) {
        Reservation reservation = findByIdAndMember(command.getId(), command.getMemberId());
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

    // ────── private helpers ──────

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다."));
    }

    private ReservationTime findTimeOrThrow(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 시간입니다."));
    }

    private Theme findThemeOrThrow(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 테마입니다."));
    }

    private Reservation findUpdatedReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new DataInconsistencyException(
                        "저장된 예약을 찾을 수 없습니다. 데이터 정합성 문제가 의심됩니다."
                ));
    }

    /**
     * 예약 ID로 조회 후 소유자 검증.
     * 다른 사람의 예약이면 404 — 존재 여부를 노출하지 않음.
     */
    private Reservation findByIdAndMember(Long reservationId, Long memberId) {
        return reservationRepository.findById(reservationId)
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
