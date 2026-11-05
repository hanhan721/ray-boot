package com.ray.common.storage.storage;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.storage.config.StorageProperties;
import com.ray.common.storage.enums.StorageType;
import com.ray.common.storage.support.LocalAccessUrlSigner;
import com.ray.common.storage.support.StorageIdentity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;

/** 临时目录与正式对象处于同一文件系统，完整写入后原子发布。 */
public class LocalStorageService implements StorageService {
    private final Path root;
    private final Path temporary;
    private final StorageProperties properties;
    private final String storageId;

    /** 解析真实根目录并建立同文件系统临时目录，以真实物理位置生成稳定存储身份。 */
    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
        try {
            // 1.将配置目录规范化为已存在的真实绝对路径，消除相对路径和符号链接别名。
            var configured = Path.of(properties.local().basePath()).toAbsolutePath().normalize();
            Files.createDirectories(configured);
            this.root = configured.toRealPath();
            // 2.在根目录内建立独立临时目录，保证完成后的发布可使用原子移动。
            this.temporary = root.resolve(".tmp");
            Files.createDirectories(temporary);
            // 3.只将物理位置纳入身份，公开访问地址和签名密钥不影响历史对象定位。
            this.storageId = StorageIdentity.fingerprint("LOCAL", root.toString());
        } catch (IOException e) {
            throw new IllegalStateException("初始化文件目录失败", e);
        }
    }

    /**
     * 完整写入临时文件并校验字节数后原子发布，不覆盖已经发布的 key。
     * 调用方负责事务外执行；普通退出会清理临时文件，崩溃遗留由孤儿清理处理。
     */
    @Override
    public void upload(InputStream input, String key, String contentType, long size) {
        Path temp = null;
        long started = System.nanoTime();
        try {
            // 1.分块写入独立临时文件，持续校验耗时及实际大小。
            temp = Files.createTempFile(temporary, "upload-", ".part");
            try (var out = Files.newOutputStream(temp)) {
                var buffer = new byte[64 * 1024];
                long count = 0;
                for (int read; (read = input.read(buffer)) != -1;) {
                    if (System.nanoTime() - started > properties.operationTimeout().toNanos()) {
                        throw new IOException("存储写入超时");
                    }
                    count += read;
                    if (count > size) throw new IOException("文件大小发生变化");
                    out.write(buffer, 0, read);
                }
                if (count != size) throw new IOException("文件未完整写入");
            }
            // 2.校验目标路径并以 CREATE_NEW 占位，已有 key 直接失败。
            var target = this.resolve(key);
            Files.createDirectories(target.getParent());
            // CREATE_NEW 占位确保不覆盖已发布 key；原子替换的只是本次占位。
            Files.createFile(target);
            try {
                // 3.原子替换本次占位；发布失败时移除占位，不遗留可访问的半成品。
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Files.deleteIfExists(target);
                throw e;
            }
        } catch (IOException e) {
            throw new BusinessException("文件存储失败: " + e.getMessage());
        } finally {
            // 4.正常结束时清理临时文件，清理失败的遗留项交给后续 GC。
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { /* 遗留交给 GC */ }
            }
        }
    }

    /** 打开已校验路径的输入流，由调用方负责关闭；字节缺失时明确失败。 */
    @Override
    public InputStream download(String key) {
        try { return Files.newInputStream(this.resolve(key)); }
        catch (IOException e) { throw new BusinessException("文件字节缺失或不可读"); }
    }
    /** 事务外删除指定 key，已不存在视为成功，其他 I/O 错误必须交由 GC 重试。 */
    @Override
    public void delete(String key) {
        try { Files.deleteIfExists(this.resolve(key)); }
        catch (IOException e) { throw new BusinessException("删除文件失败: " + e.getMessage()); }
    }
    @Override
    public boolean exists(String key) { return Files.exists(this.resolve(key)); }
    @Override
    public StorageType getStorageType() { return StorageType.LOCAL; }
    @Override
    public String getStorageId() { return storageId; }
    @Override
    public String getBucket() { return "local"; }

    /** 按逻辑文件 ID 签发 LOCAL 直链，签名同时覆盖用途与到期时间，不暴露存储 key。 */
    @Override
    public String getAccessUrl(SignRequest request) {
        // 1.按服务端选定的寿命计算到期时间，并对文件 ID、到期时间、用途计算签名。
        long exp = Instant.now().plus(request.duration()).getEpochSecond();
        var access = properties.access();
        var signer = new LocalAccessUrlSigner(access == null ? null : access.signSecret());
        var signature = signer.sign(Long.toString(request.fileId()), exp, request.purpose());
        // 2.拼接应用公开地址，后续访问由文件接口校验签名并解析真实 key。
        var base = access == null || access.publicBaseUrl() == null ? "" : access.publicBaseUrl().replaceAll("/+$", "");
        return base + "/api/sys/file/access?id=" + request.fileId() + "&exp=" + exp
                + "&purpose=" + request.purpose() + "&sig=" + signature;
    }

    /** 按 key 字典序分页扫描正式文件与临时文件，额外保留一项判断是否还有下一页。 */
    @Override
    public ObjectPage list(String cursor) {
        // 1.流式遍历目录，只保留游标后最小的 101 项，不把整个目录读入内存。
        try (var paths = Files.walk(root)) {
            var queue = new java.util.PriorityQueue<StoredObject>(101,
                    Comparator.comparing(StoredObject::key).reversed());
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                var path = iterator.next();
                // 2.跳过目录、符号链接和游标之前的文件，收集清理所需的年龄及大小。
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                var key = root.relativize(path).toString().replace('\\', '/');
                if (cursor != null && key.compareTo(cursor) <= 0) continue;
                var attr = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                queue.add(new StoredObject(key, attr.lastModifiedTime().toInstant(), attr.size()));
                if (queue.size() > 101) queue.poll();
            }
            // 3.返回排序后的前 100 项，存在额外项时以本页最后一个 key 作为下一页游标。
            var items = queue.stream().sorted(Comparator.comparing(StoredObject::key)).toList();
            return new ObjectPage(items.stream().limit(100).toList(), items.size() > 100 ? items.get(99).key() : null);
        } catch (IOException | java.io.UncheckedIOException e) {
            throw new BusinessException("扫描本地文件失败: " + e.getMessage());
        }
    }

    /** 限定 key 位于专用根目录内，并拒绝路径链上的符号链接，防止目录穿越。 */
    private Path resolve(String key) {
        // 1.规范化路径并校验根目录边界，禁止直接操作根目录。
        var resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) throw new BusinessException("非法文件路径");
        // 2.逐级拒绝符号链接；根目录由应用专用，避免外部修改绕过检查。
        for (var p = resolved; p != null && !p.equals(root); p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw new BusinessException("非法文件链接");
        }
        return resolved;
    }
}
