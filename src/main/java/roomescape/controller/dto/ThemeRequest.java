package roomescape.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import roomescape.service.dto.ThemeCreateCommand;

public class ThemeRequest {

    @NotNull(message = "매장 ID는 비어 있을 수 없습니다.")
    private Long storeId;           // 3단계 추가

    @NotBlank(message = "이름은 비어 있을 수 없습니다.")
    private String name;

    @NotBlank(message = "설명은 비어 있을 수 없습니다.")
    private String description;

    @NotBlank(message = "썸네일은 비어 있을 수 없습니다.")
    private String thumbnailUrl;

    public ThemeRequest() {
    }

    public ThemeCreateCommand toCommand() {
        return new ThemeCreateCommand(storeId, name, description, thumbnailUrl);
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
