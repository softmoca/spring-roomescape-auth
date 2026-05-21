package roomescape.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.LoginCheckInterceptor;
import roomescape.auth.LoginMemberArgumentResolver;
import roomescape.service.MemberService;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MemberService memberService;

    public WebMvcConfig(MemberService memberService) {
        this.memberService = memberService;
    }
    // /**에 걸고 예외를 빼는 방식.
    // @LoginMember 파라미터가 없으면 Interceptor 내부에서 return true로 통과하므로,
    // 화이트리스트 설정에 빠진 경로도 @LoginMember만 없으면 공개 API로 동작.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .addPathPatterns("/**")
                // 인증 불필요 경로: 로그인/로그아웃, 정적 리소스, 공개 API
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/user/themes",
                        "/user/themes/**",
                        "/admin/**"   // 어드민은 별도 권한 체계 - 3단계에서 역할 기반 인가 추가 예정
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver(memberService));
    }
}
