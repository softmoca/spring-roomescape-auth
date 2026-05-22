package roomescape.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.LoginMember;
import roomescape.controller.dto.AdminReservationRequest;
import roomescape.controller.dto.ReservationResponse;
import roomescape.domain.Member;
import roomescape.service.ReservationService;
import roomescape.service.dto.ReservationCreateCommand;
import roomescape.service.dto.ReservationResult;

/**
 * [3단계 변경]
 * - GET /admin/reservations: 전체 조회 → 매니저 담당 매장 예약만 조회로 변경
 * - DELETE /admin/reservations/{id}: 소유자 검증 없던 삭제 → 매니저 인가 검증 삭제로 변경
 * - POST /admin/reservations: 어드민 직접 예약 생성 — 현재는 그대로 유지
 *   (매니저가 생성할 경우 theme이 자기 매장 것인지 검증이 필요하지만, 현 미션 범위 외)
 */
@RequestMapping("/admin/reservations")
@RestController
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<ReservationResponse> list(@LoginMember Member member) {
        return reservationService.findByManagerStore(member.getId()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@RequestBody @Valid AdminReservationRequest request) {
        ReservationCreateCommand command = new ReservationCreateCommand(
                request.getMemberId(), request.getDate(), request.getTimeId(), request.getThemeId()
        );
        ReservationResult saved = reservationService.create(command);
        return ReservationResponse.from(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginMember Member member, @PathVariable Long id) {
        reservationService.deleteByManager(id, member.getId());
    }
}
