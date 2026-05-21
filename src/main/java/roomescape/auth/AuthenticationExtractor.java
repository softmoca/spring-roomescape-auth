package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청에서 memberId를 꺼내는 책임을 추상화한 인터페이스.
 *
 * [A] 계층 — "인증 정보를 꺼내는 층"만 교체하면 새 인증 방식을 추가할 수 있다.
 * Interceptor · ArgumentResolver · 컨트롤러 · 서비스는 이 인터페이스만 알면 된다.
 *
 * 구현체:
 *  - SessionAuthenticationExtractor : 웹 (쿠키 → 세션)
 *  - TokenAuthenticationExtractor   : 모바일 (Authorization: Bearer 헤더)
 *
 * 새 인증 방식이 생기면 구현체 하나만 추가하면 된다. (OCP)
 */
public interface AuthenticationExtractor {

    /** 이 추출기가 처리할 수 있는 요청인지 판단한다. */
    boolean supports(HttpServletRequest request);

    /**
     * 요청에서 memberId를 꺼낸다.
     * 인증 정보가 없거나 유효하지 않으면 UnauthorizedException을 던진다.
     */
    Long extractMemberId(HttpServletRequest request);
}
