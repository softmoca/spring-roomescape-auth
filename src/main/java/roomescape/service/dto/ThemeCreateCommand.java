package roomescape.service.dto;

import roomescape.exception.client.InvalidCommandException;

public class ThemeCreateCommand {

    private final Long storeId;     // 3단계 추가
    private final String name;
    private final String description;
    private final String thumbnail;

    public ThemeCreateCommand(Long storeId, String name, String description, String thumbnail) {
        validate(storeId,name, description, thumbnail);
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.thumbnail = thumbnail;
    }

    private void validate(Long id,String name, String description, String thumbnail) {
        if (id == null) {
            throw new InvalidCommandException("테마 ID는 비어 있을 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidCommandException("테마 이름은 비어 있을 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidCommandException("테마 설명은 비어 있을 수 없습니다.");
        }
        if (thumbnail == null || thumbnail.isBlank()) {
            throw new InvalidCommandException("테마 썸네일은 비어 있을 수 없습니다.");
        }
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnail() {
        return thumbnail;
    }
}
