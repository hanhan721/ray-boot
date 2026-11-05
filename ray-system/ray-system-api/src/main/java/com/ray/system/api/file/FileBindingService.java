package com.ray.system.api.file;

/** 业务 Service 完成权限校验后，在业务事务中维护文件归属。 */
public interface FileBindingService {
    /** 校验未删除、对象可用及上传者归属，将文件绑定到唯一业务记录。 */
    void bind(long fileId, String bizType, long bizId);
    /** 先移除真实业务引用，再逻辑删除关联文件；与业务事务共同回滚。 */
    void release(long fileId, String bizType, long bizId);
}
