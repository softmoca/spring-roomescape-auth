package roomescape.service.dto;

import roomescape.domain.Member;
import roomescape.domain.MemberRole;

public class MemberResult {

    private final Long id;
    private final String name;
    private final String email;
    private final MemberRole role;

    public MemberResult(Long id, String name, String email, MemberRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // password는 의도적으로 제외 - 결과 객체에 비밀번호를 싣지 않는다
    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getRole()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public MemberRole getRole() {
        return role;
    }
}
