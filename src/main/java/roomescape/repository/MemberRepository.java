package roomescape.repository;

import java.util.Optional;
import roomescape.domain.Member;

public interface MemberRepository {
    Optional<Member> findByEmail(String email);//로그인시 이메일로 조회
    Optional<Member> findById(Long id); // 세션의 memberId로 Member 조회
}
