package roomescape.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 이 API는 인증이 필요하다"는 사실을 표현할 방법이 필요했음
 * 경로 패턴 화이트리스트 방식은 새 API 추가 시 깜빡하면 뚫리는 Fail-open 문제가 있음.
 *
 * 컨트롤러 파라미터에 붙이면 ArgumentResolver가 현재 로그인 사용자를 주입해준다.
 * 이 어노테이션이 붙은 파라미터가 있는 API = 인증 필요 API.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
}
