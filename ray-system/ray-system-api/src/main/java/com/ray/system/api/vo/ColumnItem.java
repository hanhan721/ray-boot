package com.ray.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

/**
 * 列配置项
 */
@Schema(description = "列配置项")
public record ColumnItem(
        @NotBlank(message = "列标识不能为空")
        @Schema(description = "列标识", example = "username")
        String key,

        @Schema(description = "是否显示", example = "true")
        boolean visible,

        @Schema(description = "排列顺序", example = "0")
        int sort,

        @Nullable
        @Schema(description = "列宽度(px)，null表示自适应", example = "120")
        Integer width,

        @Nullable
        @Schema(description = "固定方向：left/right/null", example = "left")
        String fixed
) {
}
