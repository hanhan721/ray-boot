package com.ray.common.core.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询参数
 */
@Data
@Accessors(chain = true)
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码，从 1 开始
     */
    @Min(value = 1, message = "页码最小值为 1")
    private int pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 500, message = "每页条数最大值为 500")
    private int pageSize = 10;
}
