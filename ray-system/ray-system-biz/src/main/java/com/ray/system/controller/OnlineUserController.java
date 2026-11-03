package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.OnlineUserPageQuery;
import com.ray.system.api.domain.OnlineUserVO;
import com.ray.system.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 在线用户管理
 */
@Tag(name = "在线用户管理")
@RestController
@RequestMapping("/api/sys/online")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @Operation(summary = "按登录时间倒序分页查询在线会话")
    @GetMapping("/page")
    @PreAuthorize("@perm.has('system:online:list')")
    public R<IPage<OnlineUserVO>> page(@Valid OnlineUserPageQuery query) {
        return R.ok(onlineUserService.page(query));
    }

    @Operation(summary = "强制下线（单个会话）")
    @DeleteMapping("/{sessionId}")
    @PreAuthorize("@perm.has('system:online:kick')")
    public R<Void> kickBySessionId(@PathVariable @Pattern(regexp = "[0-9a-f]{64}", message = "会话标识格式无效") String sessionId) {
        onlineUserService.kickBySessionId(sessionId);
        return R.ok();
    }

    @Operation(summary = "强制下线（用户所有会话）")
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("@perm.has('system:online:kick')")
    public R<Void> kickByUserId(@PathVariable Long userId) {
        onlineUserService.kickByUserId(userId);
        return R.ok();
    }
}
