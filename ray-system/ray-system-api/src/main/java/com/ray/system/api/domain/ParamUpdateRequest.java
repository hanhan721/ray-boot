package com.ray.system.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 更新系统参数请求 VO
 */
@Schema(description = "更新系统参数请求")
public record ParamUpdateRequest(
        @NotNull(message = "参数ID不能为空")
        @Schema(description = "参数ID", example = "1")
        Long id,

        @NotBlank(message = "参数值不能为空")
        @Schema(description = "参数值", example = "10485760")
        String paramValue,

        @Schema(description = "是否加密")
        Boolean encrypted,

        @Schema(description = "参数描述", example = "最大上传文件大小")
        String description
) {
}
