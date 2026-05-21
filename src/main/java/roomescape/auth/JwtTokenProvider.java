package roomescape.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import roomescape.exception.client.UnauthorizedException;

/**
 * JWT 토큰 발급 · 검증 전담 컴포넌트.
 *
 * - 페이로드: memberId(Long) 하나만 담는다. 최소 정보 원칙.
 *   권한(role) 등이 필요해지면 claim을 추가하되, 그때 재검토.
 * - 만료: 미션 범위에서는 만료 없음(단순화).
 * 4단계 동시로그인 방지 학습에서 만료·무효화 전략을 검토할 예정.
 * - 서명 알고리즘: HMAC-SHA256 (HS256).
 * 비대칭 키가 필요한 경우(여러 서비스 간 토큰 공유)가 아니라면 HS256이 적합하고 구현이 단순하다.
 */
@Component
public class JwtTokenProvider {

    private static final String MEMBER_ID_CLAIM = "memberId";

    private final Key secretKey;

    public JwtTokenProvider(@Value("${security.jwt.secret-key}") String rawSecretKey) {
        // jjwt 0.11.x: Keys.hmacShaKeyFor()는 32바이트(256비트) 이상 키를 요구한다.
        // application.properties의 값이 짧으면 IllegalArgumentException 발생.
        this.secretKey = Keys.hmacShaKeyFor(rawSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * memberId를 담은 JWT를 발급한다.
     */
    public String createToken(Long memberId) {
        return Jwts.builder()
                .claim(MEMBER_ID_CLAIM, memberId)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 memberId를 꺼낸다.
     * 서명 검증 실패·파싱 오류 시 UnauthorizedException(401).
     */
    public Long extractMemberId(String token) {
        Claims claims = parseClaims(token);
        Number memberId = claims.get(MEMBER_ID_CLAIM, Number.class);
        return memberId.longValue();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }
    }
}
