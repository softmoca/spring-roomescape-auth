package roomescape.domain;

import roomescape.domain.exception.InvalidDomainException;

public class Store {

    private final Long id;
    private final String name;

    private Store(Long id, String name) {
        validateName(name);
        this.id = id;
        this.name = name;
    }

    public static Store create(String name) {
        return new Store(null, name);
    }

    public static Store reconstitute(Long id, String name) {
        return new Store(id, name);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDomainException("매장 이름은 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
