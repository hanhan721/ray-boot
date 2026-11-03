package com.ray.system.api.domain;

import com.ray.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 在线会话按登录时间倒序分页，只提供时间范围查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineUserPageQuery extends PageQuery {
    @Parameter(description = "登录时间起点（包含），ISO 日期时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Parameter(description = "登录时间终点（包含），ISO 日期时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @AssertTrue(message = "起始时间不能晚于结束时间")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || !startTime.isAfter(endTime);
    }
}
