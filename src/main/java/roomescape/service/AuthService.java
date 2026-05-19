package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.domain.Member;
import roomescape.exception.client.UnauthorizedException;
import roomescape.repository.MemberRepository;
import roomescape.service.dto.LoginCommand;

@Service
public class AuthService {

    private final MemberRepository memberRepository;

    public AuthService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 로그인: 이메일+비밀번호 검증 후, 세션에 담을 식별자(memberId)를 돌려준다
    public Long login(LoginCommand command) {
        Member member = memberRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!member.matchesPassword(command.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return member.getId();
    }

    // memberId로 현재 회원을 복원 (ArgumentResolver가 사용)
    public Member findAuthenticatedMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다."));
    }
}
