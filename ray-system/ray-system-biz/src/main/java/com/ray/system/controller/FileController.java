package com.ray.system.controller;

import com.ray.common.core.domain.R;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.storage.config.StorageProperties;
import com.ray.common.storage.support.LocalAccessUrlSigner;
import com.ray.system.api.domain.FileUploadVO;
import com.ray.system.api.entity.SysFile;
import com.ray.system.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 文件管理
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/sys/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final StorageProperties storageProperties;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) String bizType,
                                  @RequestParam(required = false) Long bizId) {
        return R.ok(toVO(fileService.upload(file, bizType, bizId)));
    }

    @Operation(summary = "批量上传文件")
    @PostMapping("/upload/batch")
    public R<List<FileUploadVO>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                             @RequestParam(required = false) String bizType,
                                             @RequestParam(required = false) Long bizId) {
        return R.ok(fileService.uploadBatch(files, bizType, bizId).stream().map(this::toVO).toList());
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download")
    public void download(@RequestParam(required = false) String bucket,
                         @RequestParam String filePath,
                         HttpServletResponse response) {
        fileService.download(bucket, filePath, response);
    }

    @Operation(summary = "签名直链访问（LOCAL）")
    @GetMapping("/access")
    public void access(@RequestParam(required = false) String bucket,
                       @RequestParam("path") String filePath,
                       @RequestParam("exp") long expiresAt,
                       @RequestParam(value = "level", defaultValue = "LOCAL") String level,
                       @RequestParam("sig") String signature,
                       HttpServletResponse response) {
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new BusinessException("访问链接已过期");
        }

        String secret = storageProperties.access() != null ? storageProperties.access().signSecret() : null;
        var signer = new LocalAccessUrlSigner(secret);
        if (!signer.verify(filePath, expiresAt, level, signature)) {
            throw new BusinessException("无效的访问签名");
        }

        fileService.access(bucket, filePath, response);
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<SysFile> getById(@PathVariable Long id) {
        return R.ok(fileService.getById(id));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    public R<Void> delete(@RequestBody @NotEmpty(message = "ID 集合不能为空") Set<Long> ids) {
        fileService.delete(ids);
        return R.ok();
    }

    private FileUploadVO toVO(SysFile file) {
        return new FileUploadVO(file.getId(), file.getFilePath(), file.getBucket(), file.getFilePath());
    }
}
