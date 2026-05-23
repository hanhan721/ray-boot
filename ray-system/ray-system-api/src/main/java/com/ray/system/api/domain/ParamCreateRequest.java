package com.ray.system.api.domain;

import com.ray.system.api.enums.ParamType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建系统参数请求 VO
 */
@Schema(description = "创建系统参数请求")
public record ParamCreateRequest(
        @NotBlank(message = "参数键不能为空")
        @Schema(description = "参数键", example = "sys.max-upload-size")
        String paramKey,

        @NotBlank(message = "参数值不能为空")
        @Schema(description = "参数值", example = "10485760")
        String paramValue,

        @NotNull(message = "参数类型不能为空")
        @Schema(description = "参数类型", example = "INTEGER")
        ParamType paramType,

        @NotNull(message = "业务分类不能为空")
        @Schema(description = "业务分类", example = "0")
        Integer bizType,

        @Schema(description = "是否加密", example = "false")
        Boolean encrypted,

        @Schema(description = "参数描述", example = "最大上传文件大小")
        String description
) {
}
