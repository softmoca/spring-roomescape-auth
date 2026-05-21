package roomescape.controller.dto;

/**
 * 모바일 로그인 성공 응답.
 * 앱이 이후 요청에서 Authorization: Bearer <token> 헤더로 보낼 토큰을 담는다.
 *
 * 웹 LoginResponse(name만)와 분리한 이유:
 *  - 웹은 쿠키로 세션을 관리하므로 클라이언트가 토큰을 저장할 필요 없음.
 *  - 모바일은 토큰을 직접 들고 다녀야 하므로 응답 본문에 토큰 포함.
 *  - 두 응답의 형태가 달라서 DTO를 분리하는 게 명확하다.
 */
public class MobileLoginResponse {

    private final String token;
    private final String name;

    public MobileLoginResponse(String token, String name) {
        this.token = token;
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }
}
