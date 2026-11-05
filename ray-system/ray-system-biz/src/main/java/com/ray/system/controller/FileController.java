package com.ray.system.controller;

import com.ray.common.core.domain.R;
import com.ray.system.api.domain.FileUploadVO;
import com.ray.system.api.domain.FileBatchResult;
import com.ray.system.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.util.List;
import java.util.Set;

/** 基于逻辑文件 ID 的通用文件接口，具体归属与访问权限由文件 Service 统一校验。 */
@RestController
@RequestMapping("/api/sys/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService files;

    /** 上传单个未绑定文件，业务归属只能在对应业务 Service 完成鉴权后建立。 */
    @PostMapping
    @PreAuthorize("@perm.has('system:file:upload')")
    public R<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(files.upload(file));
    }

    /** 按输入顺序返回逐项结果，单项失败不撤销其他成功上传。 */
    @PostMapping("/batch")
    @PreAuthorize("@perm.has('system:file:upload')")
    public R<List<FileBatchResult>> uploadBatch(@RequestParam("files") List<MultipartFile> batch) {
        return R.ok(files.uploadBatch(batch));
    }

    /** 查询当前调用者有权访问的文件详情，不暴露底层存储位置。 */
    @GetMapping("/{id}")
    public R<FileUploadVO> detail(@PathVariable long id) { return R.ok(files.detail(id)); }

    /** 签发固定寿命的浏览链接，请求参数不能提升链接寿命档位。 */
    @GetMapping("/{id}/url")
    public R<String> url(@PathVariable long id) { return R.ok(files.url(id, false)); }

    /** 完成文件鉴权后重定向至原始名称下载链接，重定向响应不缓存。 */
    @GetMapping("/{id}/download")
    public ResponseEntity<Void> download(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(files.url(id, true)))
                .header("Cache-Control", "private, no-store").build();
    }

    /** LOCAL 签名直链入口，无需登录，Service 校验签名、有效期和文件状态。 */
    @GetMapping("/access")
    public void access(@RequestParam long id, @RequestParam long exp, @RequestParam String purpose,
                       @RequestParam("sig") String signature, HttpServletResponse response) {
        files.access(id, exp, purpose, signature, response);
    }

    /** 整批校验并逻辑删除，存在真实业务引用时拒绝整批请求。 */
    @DeleteMapping
    public R<Void> delete(@RequestBody @NotEmpty Set<Long> ids) {
        files.delete(ids);
        return R.ok();
    }
}
