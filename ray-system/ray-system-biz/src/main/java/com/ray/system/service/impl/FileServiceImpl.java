package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.storage.service.FileNameResolver;
import com.ray.common.storage.storage.StorageService;
import com.ray.common.storage.util.FilePathUtils;
import com.ray.system.api.entity.SysFile;
import com.ray.system.mapper.FileMapper;
import com.ray.system.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, SysFile> implements FileService, FileNameResolver {

    private final StorageService storageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String bizType, Long bizId) {
        AssertUtils.isTrue(!file.isEmpty(), "上传文件不能为空");

        var originalName = file.getOriginalFilename();
        var filePath = FilePathUtils.generatePath(originalName);
        var fileName = FilePathUtils.getFileName(filePath);
        var fileExt = FilePathUtils.getExtension(originalName);
        var contentType = file.getContentType();

        // 存储文件
        try (var inputStream = file.getInputStream()) {
            storageService.upload(inputStream, filePath, contentType, file.getSize());
        } catch (IOException e) {
            log.error("读取上传文件失败: {}", originalName, e);
            throw new BusinessException("文件上传失败");
        }

        // 保存元信息
        var sysFile = new SysFile()
                .setOriginalName(originalName)
                .setFileName(fileName)
                .setFilePath(filePath)
                .setFileExt(fileExt)
                .setFileSize(file.getSize())
                .setContentType(contentType)
                .setStorageType(storageService.getStorageType())
                .setBucket(storageService.getBucket())
                .setBizType(bizType)
                .setBizId(bizId);
        super.save(sysFile);
        return sysFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SysFile> uploadBatch(List<MultipartFile> files, String bizType, Long bizId) {
        AssertUtils.isTrue(!CollectionUtils.isEmpty(files), "上传文件列表不能为空");

        var result = new ArrayList<SysFile>(files.size());
        for (var file : files) {
            result.add(this.upload(file, bizType, bizId));
        }
        return result;
    }

    @Override
    public void download(String bucket, String filePath, HttpServletResponse response) {
        var sysFile = super.getOne(
                Wrappers.<SysFile>lambdaQuery().eq(SysFile::getFilePath, filePath)
        );
        AssertUtils.notNull(sysFile, "文件不存在");

        try (var inputStream = storageService.download(bucket, filePath);
             var outputStream = response.getOutputStream()) {
            var encodedName = URLEncoder.encode(sysFile.getOriginalName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(sysFile.getFileSize());
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件下载失败: {}", filePath, e);
            throw new BusinessException("文件下载失败");
        }
    }

    @Override
    public void access(String bucket, String filePath, HttpServletResponse response) {
        var sysFile = super.getOne(
                Wrappers.<SysFile>lambdaQuery().eq(SysFile::getFilePath, filePath)
        );

        try (var inputStream = storageService.download(bucket, filePath);
             var outputStream = response.getOutputStream()) {
            String contentType = sysFile != null && StringUtils.hasText(sysFile.getContentType())
                    ? sysFile.getContentType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            response.setContentType(contentType);
            if (sysFile != null && sysFile.getFileSize() != null && sysFile.getFileSize() > 0) {
                response.setContentLengthLong(sysFile.getFileSize());
            }
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件访问失败: {}", filePath, e);
            throw new BusinessException("文件访问失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        var files = super.listByIds(ids);
        if (CollectionUtils.isEmpty(files)) {
            return;
        }

        // 批量删除物理文件
        var filePaths = files.stream().map(SysFile::getFilePath).toList();
        try {
            storageService.batchDelete(filePaths);
        } catch (Exception e) {
            log.warn("物理文件批量删除失败，继续删除数据库记录: count={}", filePaths.size(), e);
        }

        // 逻辑删除数据库记录
        super.removeByIds(ids);
    }

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByFilePaths(String bucket, List<String> filePaths) {
        if (CollectionUtils.isEmpty(filePaths)) {
            return;
        }

        // 查询文件记录
        var files = super.list(
                Wrappers.<SysFile>lambdaQuery().in(SysFile::getFilePath, filePaths)
        );

        // 批量删除物理文件
        try {
            storageService.batchDelete(bucket, filePaths);
        } catch (Exception e) {
            log.warn("物理文件批量删除失败，继续删除数据库记录: bucket={}, count={}", bucket, filePaths.size(), e);
        }

        // 逻辑删除数据库记录
        if (!CollectionUtils.isEmpty(files)) {
            var ids = files.stream().map(SysFile::getId).toList();
            super.removeByIds(ids);
        }
    }

    @Override
    public Map<String, String> resolve(Collection<String> filePaths) {
        if (CollectionUtils.isEmpty(filePaths)) {
            return Collections.emptyMap();
        }
        return super.list(
                        Wrappers.<SysFile>lambdaQuery()
                                .select(SysFile::getFilePath, SysFile::getOriginalName)
                                .in(SysFile::getFilePath, filePaths)
                ).stream()
                .collect(Collectors.toMap(SysFile::getFilePath, SysFile::getOriginalName, (a, b) -> a));
    }

}
