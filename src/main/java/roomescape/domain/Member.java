package roomescape.domain;

import roomescape.domain.exception.InvalidDomainException;

public class Member {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;

    private Member(Long id, String email, String password, String name) {
        validateEmail(email);
        validatePassword(password);
        validateName(name);
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    public static Member create(String email, String password, String name) {
        return new Member(null, email, password, name);
    }

    public static Member reconstitute(Long id, String email, String password, String name) {
        return new Member(id, email, password, name);
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidDomainException("이메일은 비어 있을 수 없습니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidDomainException("비밀번호는 비어 있을 수 없습니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDomainException("이름은 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }
}
