package roomescape.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.LoginMember;
import roomescape.controller.dto.ReservationRequest;
import roomescape.controller.dto.ReservationResponse;
import roomescape.controller.dto.ReservationUpdateRequest;
import roomescape.domain.Member;
import roomescape.service.ReservationService;
import roomescape.service.dto.ReservationResult;

@RestController
@RequestMapping("/user/reservations")
public class UserReservationController {

    private final ReservationService reservationService;

    public UserReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @LoginMember Member member,
            @RequestBody @Valid ReservationRequest request) {
        ReservationResult saved = reservationService.create(request.toCommand(member.getId()));
        return ReservationResponse.from(saved);
    }

    @GetMapping
    public List<ReservationResponse> listMine(@LoginMember Member member) {
        return reservationService.findByMemberId(member.getId()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @LoginMember Member member) {
        reservationService.deleteByOwner(id, member.getId());
    }

    @PatchMapping("/{id}")
    public ReservationResponse update(
            @PathVariable Long id,
            @LoginMember Member member,
            @RequestBody @Valid ReservationUpdateRequest request
    ) {
        ReservationResult updated = reservationService.updateByOwner(request.toCommand(id, member.getId()));
        return ReservationResponse.from(updated);
    }


}
