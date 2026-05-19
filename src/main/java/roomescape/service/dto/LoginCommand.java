package roomescape.service.dto;

import roomescape.exception.client.InvalidCommandException;

public class LoginCommand {
    private final String email;
    private final String password;

    public LoginCommand(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new InvalidCommandException("이메일은 비어 있을 수 없습니다.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidCommandException("비밀번호는 비어 있을 수 없습니다.");
        }
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
