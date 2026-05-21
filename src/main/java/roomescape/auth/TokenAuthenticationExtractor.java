package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import roomescape.exception.client.UnauthorizedException;

/**
 * 모바일 클라이언트용 추출기 — Authorization: Bearer <token> 헤더에서 memberId를 꺼낸다.
 *
 * supports() 기준: Authorization 헤더가 존재하고 "Bearer "로 시작하면 처리 가능.
 *   → 세션 기반 웹 요청에는 이 헤더가 없으므로 자동으로 건너뜀.
 *
 * 헤더 형식: Authorization: Bearer eyJhbGci...
 *   - "Bearer " 접두사(7자)를 제거한 나머지가 JWT.
 */
@Component
public class TokenAuthenticationExtractor implements AuthenticationExtractor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public TokenAuthenticationExtractor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean supports(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    @Override
    public Long extractMemberId(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        return jwtTokenProvider.extractMemberId(token);
    }
}
