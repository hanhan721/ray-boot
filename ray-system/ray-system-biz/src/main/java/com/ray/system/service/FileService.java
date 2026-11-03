package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.SysFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * 文件 Service
 */
public interface FileService extends IService<SysFile> {

    /**
     * 上传文件
     */
    SysFile upload(MultipartFile file, String bizType, Long bizId);

    /**
     * 批量上传文件
     */
    List<SysFile> uploadBatch(List<MultipartFile> files, String bizType, Long bizId);

    /**
     * 下载文件
     *
     * @param filePath 存储路径
     */
    void download(String filePath, HttpServletResponse response);

    /**
     * 直接访问文件（用于签名直链）
     */
    void access(String filePath, HttpServletResponse response);

    /**
     * 删除文件
     */
    void delete(Set<Long> ids);

    /**
     * 异步批量删除文件（物理文件 + 数据库记录）
     *
     * @param filePaths 存储路径列表
     */
    void deleteByFilePaths(List<String> filePaths);
}
