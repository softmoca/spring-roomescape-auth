package roomescape.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.JwtTokenProvider;
import roomescape.controller.dto.LoginRequest;
import roomescape.controller.dto.MobileLoginResponse;
import roomescape.domain.Member;
import roomescape.service.MemberService;

/**
 * 모바일 앱 전용 로그인 엔드포인트.
 *
 * POST /api/login
 *   - 요청: LoginRequest (email, password) — 웹과 동일한 DTO 재사용
 *   - 응답: MobileLoginResponse { token, name }
 *     앱은 이 토큰을 저장해두고 이후 요청마다
 *     Authorization: Bearer <token> 헤더로 전달한다.
 *
 * 웹 /login과 분리한 이유:
 * - 웹 /login → 응답 본문에 { "name": "브라운" } + Set-Cookie: JSESSIONID=...
 * - 모바일 /api/login → 응답 본문에 { "token": "eyJ...", "name": "브라운" }
 *   - 웹은 응답에 Set-Cookie로 세션을 심어주는 반면,
 *     모바일은 응답 본문의 토큰을 앱이 직접 저장해야 한다.
 *   - 같은 엔드포인트에서 클라이언트 종류를 분기하면
 *     "컨트롤러가 클라이언트 종류를 알아야 하는" 문제가 생긴다.
 *   - 진입점 분리로 웹/모바일 흐름을 각자 명확하게 유지한다.
 */
@RestController
@RequestMapping("/api")
public class MobileLoginController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    public MobileLoginController(
            MemberService memberService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public MobileLoginResponse login(@RequestBody @Valid LoginRequest request) {
        Member member = memberService.login(request.getEmail(), request.getPassword());
        String token = jwtTokenProvider.createToken(member.getId());
        return new MobileLoginResponse(token, member.getName());
    }
}
