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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.LoginMember;
import roomescape.controller.dto.ReservationRequest;
import roomescape.controller.dto.ReservationResponse;
import roomescape.controller.dto.ReservationUpdateRequest;
import roomescape.domain.Member;
import roomescape.service.ReservationService;
import roomescape.service.dto.ReservationCreateCommand;
import roomescape.service.dto.ReservationResult;
import roomescape.service.dto.ReservationUpdateCommand;

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
            @RequestBody @Valid ReservationRequest request
    ) {
        ReservationCreateCommand command = new ReservationCreateCommand(
                member.getId(), request.getDate(), request.getTimeId(), request.getThemeId()
        );
        ReservationResult saved = reservationService.create(command);
        return ReservationResponse.from(saved);
    }

    @GetMapping
    public List<ReservationResponse> list(@LoginMember Member member) {
        return reservationService.findByMember(member.getId()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@LoginMember Member member, @PathVariable Long id) {
        reservationService.deleteByOwner(id, member.getId());
    }

    @PatchMapping("/{id}")
    public ReservationResponse update(
            @LoginMember Member member,
            @PathVariable Long id,
            @RequestBody @Valid ReservationUpdateRequest request
    ) {
        ReservationUpdateCommand command = new ReservationUpdateCommand(
                id, member.getId(), request.getDate(), request.getTimeId()
        );
        ReservationResult updated = reservationService.updateByOwner(command);
        return ReservationResponse.from(updated);
    }
}
