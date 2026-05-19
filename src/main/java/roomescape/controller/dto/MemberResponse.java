package roomescape.controller.dto;

import roomescape.service.dto.MemberResult;

public class MemberResponse {

    private final Long id;
    private final String name;

    public MemberResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // 응답에는 id와 name만 - email, role, password는 노출하지 않는다
    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(result.getId(), result.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
