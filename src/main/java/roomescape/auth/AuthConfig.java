package roomescape.auth;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.service.AuthService;

@Configuration
public class AuthConfig implements WebMvcConfigurer {

    private final AuthService authService;

    public AuthConfig(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .addPathPatterns("/user/reservations/**")  // 인증 필요: 사용자 예약
                .order(1);
        // /login, /logout — 인증의 입구. 막으면 닭-달걀 모순
        // /user/themes/**  — 테마/인기/가능시간 조회는 비로그인 둘러보기 허용
        // /admin/**        — 인가는 3단계 주제. 1단계에선 건드리지 않음
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver(authService));
    }
}
