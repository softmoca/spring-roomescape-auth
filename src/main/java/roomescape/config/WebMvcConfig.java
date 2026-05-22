package roomescape.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.AuthenticationExtractor;
import roomescape.auth.LoginCheckInterceptor;
import roomescape.auth.LoginMemberArgumentResolver;
import roomescape.auth.ManagerCheckInterceptor;
import roomescape.repository.ManagerRepository;
import roomescape.service.MemberService;


/**
 * [변경점 — 2단계]
 * - List<AuthenticationExtractor>를 생성자 주입받아 Interceptor · ArgumentResolver에 전달.
 * - Spring이 @Component 붙은 구현체(Session/Token)를 자동 수집해서 List로 주입.
 * - /api/login 경로를 excludePathPatterns에 추가 (모바일 로그인 엔드포인트).
 *
 *  * [변경점 — 3단계]
 *  * - /admin/**를 excludePathPatterns에서 제거.
 *  *   기존: /admin/**를 인증 체크 예외로 두어 누구나 접근 가능했음.
 *  *   변경: ManagerCheckInterceptor가 /admin/**에 대해 매니저 역할을 확인.
 *  * - ManagerCheckInterceptor 추가: /admin/** 진입 시 매니저 역할 거친 체.
 *  *   인터셉터 순서: LoginCheckInterceptor → ManagerCheckInterceptor.
 *  *   단, /admin/**는 LoginCheckInterceptor의 excludePathPatterns에 있으므로
 *  *   LoginCheckInterceptor는 통과하고, ManagerCheckInterceptor가 독립적으로 인증+인가를 모두 확인.
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final List<AuthenticationExtractor> extractors;
    private final MemberService memberService;
    private final ManagerRepository managerRepository;

    public WebMvcConfig(
            List<AuthenticationExtractor> extractors,
            MemberService memberService,
            ManagerRepository managerRepository
    ) {
        this.extractors = extractors;
        this.memberService = memberService;
        this.managerRepository = managerRepository;
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
                        "/admin/**"   // ManagerCheckInterceptor가 별도로 처리
                );

        // 3단계 인가 인터셉터 — /admin/** 거친 체 (매니저 역할 확인)
        // 세밀한 인가(자기 매장 예약인지)는 Service에서 처리
        registry.addInterceptor(new ManagerCheckInterceptor(extractors, managerRepository))
                .addPathPatterns("/admin/**");

    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver(extractors,memberService));
    }
}
