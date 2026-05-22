package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.exception.client.ForbiddenException;
import roomescape.exception.client.UnauthorizedException;
import roomescape.repository.ManagerRepository;

/**
 * 거친 체 인가 Interceptor — /admin/** 진입 시 매니저 역할을 확인한다.
 *
 * 역할:
 *   - 1차: 비로그인 → 401 (LoginCheckInterceptor와 역할 분리를 위해 여기서도 체크)
 *   - 2차: 로그인했지만 매니저가 아님 → 403
 *
 * 이 Interceptor가 막는 것:
 *   - "이 사람이 매니저인가?" (역할 체크) — 요청 정보만으로 DB 1회 조회로 판단 가능
 *
 * 이 Interceptor가 막지 않는 것:
 *   - "이 예약이 이 매니저의 매장 것인가?" (세밀한 인가) — 예약을 조회해야 알 수 있으므로 Service 담당
 *
 * 다층 방어:
 *   LoginCheckInterceptor(인증) → ManagerCheckInterceptor(역할 거친 체) → Service(매장 고운 체)
 */
public class ManagerCheckInterceptor implements HandlerInterceptor {

    private final List<AuthenticationExtractor> extractors;
    private final ManagerRepository managerRepository;

    public ManagerCheckInterceptor(
            List<AuthenticationExtractor> extractors,
            ManagerRepository managerRepository
    ) {
        this.extractors = extractors;
        this.managerRepository = managerRepository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1차: 인증 확인 — 비로그인이면 401
        Long memberId = extractors.stream()
                .filter(e -> e.supports(request))
                .findFirst()
                .map(e -> e.extractMemberId(request))
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다."));

        // 2차: 매니저 역할 확인 — 매니저가 아니면 403
        managerRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ForbiddenException("매니저 권한이 필요합니다."));

        return true;
    }
}
