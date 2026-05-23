package com.ray.system.api.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ray.system.api.enums.ParamType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 系统参数响应 VO
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统参数信息")
public class ParamVO {

    @Schema(description = "参数ID")
    private Long id;

    @Schema(description = "参数键", example = "sys.max-upload-size")
    private String paramKey;

    @Schema(description = "参数值（加密参数显示 ****）", example = "10485760")
    private String paramValue;

    @Schema(description = "参数类型", example = "INTEGER")
    private ParamType paramType;

    @Schema(description = "业务分类", example = "0")
    private Integer bizType;

    @Schema(description = "是否加密", example = "false")
    private Boolean encrypted;

    @Schema(description = "参数描述")
    private String description;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "更新人ID")
    private Long updateBy;
}
