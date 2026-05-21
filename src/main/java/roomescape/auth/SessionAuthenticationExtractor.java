package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import roomescape.controller.LoginController;
import roomescape.exception.client.UnauthorizedException;

/**
 * 웹 클라이언트용 추출기 — 쿠키로 전달된 세션에서 memberId를 꺼낸다.
 *
 * supports() 기준: 세션이 존재하고 SESSION_KEY 속성이 있으면 처리 가능.
 *   → 세션이 없거나 속성이 없는 요청은 다른 추출기(토큰)에게 넘긴다.
 *
 * 1단계에서 LoginCheckInterceptor · LoginMemberArgumentResolver가 직접 하던
 * session.getAttribute(SESSION_KEY) 로직을 이 클래스로 이전.
 */
@Component
public class SessionAuthenticationExtractor implements AuthenticationExtractor {

    @Override
    public boolean supports(HttpServletRequest request) {
        HttpSession session = request.getSession(false);// 세션이 없으면 새로 만들지 말고 null을 반환하라
        return session != null && session.getAttribute(LoginController.SESSION_KEY) != null;
    }

    @Override
    public Long extractMemberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        Long memberId = (Long) session.getAttribute(LoginController.SESSION_KEY);
        if (memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return memberId;
    }
}
