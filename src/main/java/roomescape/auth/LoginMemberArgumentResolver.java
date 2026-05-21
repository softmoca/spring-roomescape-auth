package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.controller.LoginController;
import roomescape.domain.Member;
import roomescape.exception.client.UnauthorizedException;
import roomescape.service.MemberService;

/**
 * 2차 안전망 — @LoginMember 파라미터에 현재 로그인 사용자를 주입한다.
 *
 * Interceptor 설정에서 경로를 빠뜨린 경우에도, 이 resolver가
 * 세션/사용자를 못 찾으면 UnauthorizedException을 던져
 * NPE 500 대신 깔끔한 401이 반환되도록 보장한다.
 */
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberService memberService;

    public LoginMemberArgumentResolver(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {// @LoginMember가 붙고 타입이 Member인 파라미터만 처리
        return parameter.hasParameterAnnotation(LoginMember.class)
                && Member.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Long memberId = (Long) session.getAttribute(LoginController.SESSION_KEY);
        if (memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return memberService.findById(memberId);
    }
}
