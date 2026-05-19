package roomescape.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import roomescape.controller.dto.LoginRequest;
import roomescape.service.AuthService;
import roomescape.service.dto.LoginCommand;

@RestController
public class AuthController {

    private static final String LOGIN_MEMBER_ID = "loginMemberId";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public void login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        Long memberId = authService.login(
                new LoginCommand(request.getEmail(), request.getPassword())
        );
        HttpSession session = httpRequest.getSession();   // 없으면 생성
        session.setAttribute(LOGIN_MEMBER_ID, memberId);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);  // 없으면 null
        if (session != null) {
            session.invalidate();
        }
    }
}
