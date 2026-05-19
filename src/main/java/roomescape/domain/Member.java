package roomescape.domain;

import roomescape.domain.exception.InvalidDomainException;

public class Member {

    private static final int MAX_NAME_LENGTH = 30;

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final MemberRole role;

    private Member(Long id, String email, String password, String name, MemberRole role) {
        validate(email, password, name, role);
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    // 새 회원 생성 (저장 전)
    public static Member create(String email, String password, String name, MemberRole role) {
        return new Member(null, email, password, name, role);
    }

    // DB 재구성 (저장 후)
    public static Member reconstitute(Long id, String email, String password,
                                      String name, MemberRole role) {
        return new Member(id, email, password, name, role);
    }

    private static void validate(String email, String password, String name, MemberRole role) {
        if (email == null || email.isBlank()) {
            throw new InvalidDomainException("이메일은 비어 있을 수 없습니다.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidDomainException("비밀번호는 비어 있을 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidDomainException("이름은 비어 있을 수 없습니다.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidDomainException("이름은 " + MAX_NAME_LENGTH + "자를 초과할 수 없습니다.");
        }
        if (role == null) {
            throw new InvalidDomainException("회원 권한은 비어 있을 수 없습니다.");
        }
    }

    // 로그인 검증 - 비밀번호 일치 여부를 도메인이 책임진다
    public boolean matchesPassword(String rawPassword) {
        return this.password.equals(rawPassword);
    }

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public MemberRole getRole() {
        return role;
    }

    // password는 getter를 두지 않는다 - 밖으로 새지 않게
}
