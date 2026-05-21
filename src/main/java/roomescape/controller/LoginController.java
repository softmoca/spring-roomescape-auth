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
import roomescape.controller.dto.LoginResponse;
import roomescape.domain.Member;
import roomescape.service.MemberService;

@RestController
public class LoginController {

    public static final String SESSION_KEY = "LOGIN_MEMBER_ID";

    private final MemberService memberService;

    public LoginController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Member member = memberService.login(request.getEmail(), request.getPassword());

        // 세션 고정 공격 방어: 로그인 성공 시 새 세션 발급
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();// 기존 세션 파기
        }
        session = httpRequest.getSession(true); // 새 세션 발금
        session.setAttribute(SESSION_KEY, member.getId());

        return new LoginResponse(member.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();// 세션 파기
        }
    }
}
