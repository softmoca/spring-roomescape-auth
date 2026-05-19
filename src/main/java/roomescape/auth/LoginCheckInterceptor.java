package roomescape.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.exception.client.UnauthorizedException;

public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final String LOGIN_MEMBER_ID = "loginMemberId";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute(LOGIN_MEMBER_ID) == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return true;
    }
}
