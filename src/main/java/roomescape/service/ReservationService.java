package roomescape.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.domain.Manager;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.policy.ReservationPolicy;
import roomescape.exception.client.BusinessRuleViolationException;
import roomescape.exception.client.ForbiddenException;
import roomescape.exception.client.ResourceNotFoundException;
import roomescape.exception.server.DataInconsistencyException;
import roomescape.repository.ManagerRepository;
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
    private final ManagerRepository managerRepository;
    private final ReservationPolicy reservationPolicy;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            ThemeRepository themeRepository,
            MemberRepository memberRepository,
            ManagerRepository managerRepository,
            ReservationPolicy reservationPolicy
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.memberRepository = memberRepository;
        this.managerRepository = managerRepository;
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

    // ────── 매니저 인가 메서드 (3단계 신규) ──────

    /**
     * 매니저가 자기 매장의 예약 목록을 조회한다.
     * 거친 체(역할 체크)는 Interceptor가, 고운 체(매장 범위 조회)는 여기서 처리한다.
     *
     * @param memberId 로그인한 매니저의 memberId
     */
    public List<ReservationResult> findByManagerStore(Long memberId) {
        Manager manager = findManagerOrThrow(memberId);
        return reservationRepository.findAll().stream()
                .filter(r -> r.getTheme().getStoreId().equals(manager.getStoreId()))
                .map(ReservationResult::from)
                .toList();
    }

    /**
     * 매니저가 자기 매장의 예약을 삭제한다.
     * Manager.canManage()로 도메인 메서드에 인가 판단을 위임 (B + C 조합).
     * 다른 매장 예약 접근 시 403 — 존재 사실을 숨기지 않는 매니저 영역 정책.
     *
     * @param reservationId 삭제할 예약 ID
     * @param memberId      로그인한 매니저의 memberId
     */
    public void deleteByManager(Long reservationId, Long memberId) {
        Manager manager = findManagerOrThrow(memberId);
        Reservation reservation = findReservationOrThrow(reservationId);

        if (!manager.canManage(reservation)) {
            throw new ForbiddenException("다른 매장의 예약에는 접근할 수 없습니다.");
        }

        reservationRepository.deleteById(reservationId);
    }

    // ────── 사용자 인가 메서드 ──────
    /** 사용자 본인 예약 조회 */
    public List<ReservationResult> findByMember(Long memberId) {
        return reservationRepository.findByMemberIdOrderByDateAscTimeAsc(memberId).stream()
                .map(ReservationResult::from)
                .toList();
    }

    /**
     * 사용자 본인 예약 취소.
     * 다른 사람의 예약이면 404 — 존재 여부를 노출하지 않는 사용자 영역 정책 (3.4 C안).
     */
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

    private Manager findManagerOrThrow(Long memberId) {
        return managerRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ForbiddenException("매니저 권한이 없습니다."));
    }

    private Reservation findReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 예약입니다."));
    }

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
     * 다른 사람의 예약이면 404 — 존재 여부를 노출하지 않음 (사용자 영역 정책).
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
