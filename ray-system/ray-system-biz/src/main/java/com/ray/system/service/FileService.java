package com.ray.system.service;

import com.ray.common.storage.service.FileResponseResolver;
import com.ray.system.api.domain.FileUploadVO;
import com.ray.system.api.domain.FileBatchResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Set;

/** 不暴露通用 CRUD，禁止绕过发布流程新增引用或恢复已删除文件。 */
public interface FileService extends FileResponseResolver {
    /** 在事务外写入字节并独立发布元数据，上传结果尚未绑定业务。 */
    FileUploadVO upload(MultipartFile file);
    /** 逐项独立上传并保留输入顺序，单项失败不撤销其他成功项。 */
    List<FileBatchResult> uploadBatch(List<MultipartFile> files);
    /** 校验上传者或查询管理权限，返回可公开的逻辑文件信息。 */
    FileUploadVO detail(long id);
    /** 鉴权后按浏览或下载用途签发固定寿命链接，不接受调用方任意提升寿命。 */
    String url(long id, boolean download);
    /** LOCAL 直链入口，校验 ID、用途和到期时间的签名后输出字节。 */
    void access(long id, long exp, String purpose, String signature, HttpServletResponse response);
    /** 整批阻断式逻辑删除，真实业务引用存在或归属异常时拒绝删除。 */
    void delete(Set<Long> ids);
}
