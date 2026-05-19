package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.domain.Member;
import roomescape.exception.client.UnauthorizedException;
import roomescape.service.AuthService;

public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String LOGIN_MEMBER_ID = "loginMemberId";

    private final AuthService authService;

    public LoginMemberArgumentResolver(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginMember.class);
        boolean isMemberType = Member.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isMemberType;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        Long memberId = (Long) session.getAttribute(LOGIN_MEMBER_ID);
        if (memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return authService.findAuthenticatedMember(memberId);
    }
}
