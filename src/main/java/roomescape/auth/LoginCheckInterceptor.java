package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.controller.LoginController;
import roomescape.exception.client.UnauthorizedException;

/**
 * 1차 방어선 — 인증이 필요한 API에 비로그인 요청이 오면 차단한다.
 *
 * 판단 기준: 컨트롤러 파라미터에 @LoginMember가 있으면 인증 필요.
 *   → 인증 요구 여부가 API 정의 바로 옆에 있어 응집도를 높인다.
 *   → 새 API 추가 시 @LoginMember 파라미터를 빠뜨리면 ArgumentResolver가 2차로 잡아준다.
 *
 * 주의: response.setStatus(401) + return false 방식은 응답 본문이 비어 사용 안 함.
 *   대신 UnauthorizedException을 던져 GlobalExceptionHandler가 처리하게 한다.
 *
 *  [변경점 — 2단계]
 *  * 기존: session.getAttribute(SESSION_KEY)를 직접 호출.
 *  * 변경: List<AuthenticationExtractor>에 위임. 어느 추출기도 supports()하지 않으면
 *  *       UnauthorizedException(401).
 *  *
 *  * 새 인증 방식이 생기면 AuthenticationExtractor 구현체만 추가하면 된다.
 *  * 이 클래스는 손대지 않아도 됨. (OCP)
 *
 */
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final List<AuthenticationExtractor> extractors;

    public LoginCheckInterceptor(List<AuthenticationExtractor> extractors) {
        this.extractors = extractors;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler//요청을 처리할 핸들러(항상 컨트롤러 메서드는 아니다)
    ) {

        //HandlerMethod는 요청을 처리할 컨트롤러 메서드를 객체로 표현한 것
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;// 컨트롤러 메서드가 아니라면 로그인 검사 대상이 아니라고 보고  정적 리소스 등은 통과
        }

        boolean requiresLogin = hasLoginMemberParameter(handlerMethod);
        if (!requiresLogin) {
            return true; // @LoginMember 파라미터 없으면 공개 API
        }

        // 어느 추출기든 supports()하는 게 있으면 인증 OK
        boolean authenticated = extractors.stream()
                .anyMatch(extractor -> extractor.supports(request));

        if (!authenticated) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }



        return true;
    }

    private boolean hasLoginMemberParameter(HandlerMethod handlerMethod) {
        return java.util.Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(param -> param.hasParameterAnnotation(LoginMember.class));
    }
}
