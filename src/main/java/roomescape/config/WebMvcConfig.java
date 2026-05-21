package roomescape.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.AuthenticationExtractor;
import roomescape.auth.LoginCheckInterceptor;
import roomescape.auth.LoginMemberArgumentResolver;
import roomescape.service.MemberService;


/**
 * [변경점 — 2단계]
 * - List<AuthenticationExtractor>를 생성자 주입받아 Interceptor · ArgumentResolver에 전달.
 * - Spring이 @Component 붙은 구현체(Session/Token)를 자동 수집해서 List로 주입.
 * - /api/login 경로를 excludePathPatterns에 추가 (모바일 로그인 엔드포인트).
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final List<AuthenticationExtractor> extractors;
    private final MemberService memberService;

    public WebMvcConfig(
            List<AuthenticationExtractor> extractors,
            MemberService memberService
    ) {
        this.extractors = extractors;
        this.memberService = memberService;
    }

    // /**에 걸고 예외를 빼는 방식.
    // @LoginMember 파라미터가 없으면 Interceptor 내부에서 return true로 통과하므로,
    // 화이트리스트 설정에 빠진 경로도 @LoginMember만 없으면 공개 API로 동작.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor(extractors))
                .addPathPatterns("/**")
                // 인증 불필요 경로: 로그인/로그아웃, 정적 리소스, 공개 API
                .excludePathPatterns(
                        "/login",
                        "/api/login",
                        "/logout",
                        "/user/themes",
                        "/user/themes/**",
                        "/admin/**"   // 어드민은 별도 권한 체계 - 3단계에서 역할 기반 인가 추가 예정
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver(extractors,memberService));
    }
}
