package com.ray.system.service.impl;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.base.Ticker;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.evaluator.PermissionEvaluator;
import com.ray.common.security.util.SecurityUtils;
import com.ray.common.storage.annotation.AccessLevel;
import com.ray.common.storage.config.StorageProperties;
import com.ray.common.storage.enums.StorageType;
import com.ray.common.storage.storage.StorageService;
import com.ray.common.storage.support.LocalAccessUrlSigner;
import com.ray.common.storage.util.FileContentUtils;
import com.ray.common.storage.util.FilePathUtils;
import com.ray.system.api.domain.*;
import com.ray.system.api.entity.*;
import com.ray.system.file.*;
import com.ray.system.mapper.FileMapper;
import com.ray.system.mapper.FileObjectMapper;
import com.ray.system.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件上传、访问与逻辑删除入口。
 * 字节写入在事务外执行，物理对象与逻辑文件通过独立短事务一起发布。
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private final FileMapper files;
    private final FileObjectMapper objects;
    private final StorageService storage;
    private final FileTransactions transactions;
    private final FileBindings bindings;
    private final FileRetention retention;
    private final PermissionEvaluator permissions;
    private final StorageProperties properties;
    private final Semaphore uploads;
    private final Ticker ticker = Ticker.systemTicker();
    private final Cache<UrlKey, SignedUrl> urls = CacheBuilder.newBuilder().maximumSize(10_000)
            .expireAfterWrite(7, TimeUnit.DAYS).build();

    public FileServiceImpl(FileMapper files, FileObjectMapper objects, StorageService storage,
                           FileTransactions transactions, FileBindings bindings, FileRetention retention,
                           PermissionEvaluator permissions, StorageProperties properties) {
        this.files = files;
        this.objects = objects;
        this.storage = storage;
        this.transactions = transactions;
        this.bindings = bindings;
        this.retention = retention;
        this.permissions = permissions;
        this.properties = properties;
        this.uploads = new Semaphore(properties.uploadConcurrency());
    }

    /** 上传单个未绑定文件；调用方不得处于业务事务或事务同步上下文中。 */
    @Override
    public FileUploadVO upload(MultipartFile file) { return this.uploadOne(file); }

    /** 逐项独立发布，保留输入顺序；单项失败转换为结果，不回滚已成功的文件。 */
    @Override
    public List<FileBatchResult> uploadBatch(List<MultipartFile> batch) {
        // 1.拒绝外层事务，并校验批次非空，避免整批上传共享数据库事务。
        this.requireOutsideTransaction();
        if (batch == null || batch.isEmpty()) throw new BusinessException("上传文件列表不能为空");
        var result = new ArrayList<FileBatchResult>(batch.size());
        // 2.逐项执行上传，分别记录成功数据或稳定错误码。
        for (int i = 0; i < batch.size(); i++) {
            var file = batch.get(i);
            try {
                result.add(new FileBatchResult(i, file == null ? null : file.getOriginalFilename(), true,
                        this.uploadOne(file), null, null));
            } catch (UploadFailure e) {
                result.add(new FileBatchResult(i, file == null ? null : file.getOriginalFilename(), false,
                        null, e.code, e.getMessage()));
            } catch (RuntimeException e) {
                log.error("文件批量上传失败: index={}", i, e);
                result.add(new FileBatchResult(i, file == null ? null : file.getOriginalFilename(), false,
                        null, "UPLOAD_FAILED", "上传失败，请重试"));
            }
        }
        return result;
    }

    /**
     * 检测内容并发布一份逻辑文件；同内容可复用物理对象，但每次上传都有独立文件 ID。
     * 首次查重仅是优化，最终复用对象由 hash 锁内的第二次查询决定。
     */
    private FileUploadVO uploadOne(MultipartFile multipart) {
        // 1.校验调用上下文和登录身份，固定本次保留期快照并申请上传名额。
        this.requireOutsideTransaction();
        var actor = SecurityUtils.getUserId();
        if (actor == null) throw new AccessDeniedException("请先登录");
        long started = ticker.read();
        var orphanRetention = Duration.ofDays(retention.orphanDays());
        if (!uploads.tryAcquire()) throw new UploadFailure("UPLOAD_BUSY", "上传并发已满，请稍后重试");
        try {
            // 2.流式检测 MIME、SHA-256 与实际大小，不将完整文件读入堆内存。
            var inspected = this.inspect(multipart);
            // 3.无锁预查可用对象；未命中时，先在事务外完整写入新 key。
            String writtenKey = null;
            if (objects.available(storage.getStorageId(), inspected.hash()) == null) {
                writtenKey = this.write(multipart, inspected);
            }
            for (;;) {
                this.checkElapsed(started, orphanRetention);
                var key = writtenKey;
                // 4.独立短事务内取得 hash 锁并重新查重，原子发布物理对象与逻辑文件。
                var file = transactions.execute(() -> {
                    transactions.lock(storage.getStorageId(), inspected.hash());
                    this.checkElapsed(started, orphanRetention);
                    var object = objects.available(storage.getStorageId(), inspected.hash());
                    if (object == null) {
                        if (key == null) return null;
                        object = new SysFileObject().setStorageId(storage.getStorageId())
                                .setStorageType(storage.getStorageType()).setBucket(storage.getBucket())
                                .setFileHash(inspected.hash()).setObjectKey(key).setFileSize(inspected.size())
                                .setContentType(inspected.contentType()).setStatus("AVAILABLE");
                        objects.insert(object);
                    } else {
                        // 复用后仍存在活跃引用，清除之前的零引用观察时间。
                        objects.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysFileObject>()
                                .eq(SysFileObject::getId, object.getId()).set(SysFileObject::getUnreferencedSince, null));
                    }
                    var logical = new SysFile().setObjectId(object.getId())
                            .setOriginalName(inspected.name()).setFileExt(inspected.ext()).setCreateBy(actor);
                    files.insert(logical);
                    return logical;
                });
                // 5.提交成功后返回结果；预查对象若已进入回收，退出事务补写后再次发布。
                if (file != null) return this.detail(file.getId());
                writtenKey = this.write(multipart, inspected);
            }
        } finally {
            // 6.无论检测、存储还是发布失败，都释放本次上传名额。
            uploads.release();
        }
    }

    /** 显式检查事务边界，防止上传期间占用数据库连接。 */
    private void requireOutsideTransaction() {
        // NEVER 仍会建立同步上下文，使首次查重连接滞留到方法退出，因此不使用事务注解。
        if (TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("上传必须在业务事务与事务同步上下文外调用");
        }
    }

    /** 校验名称和大小，并从实际字节计算摘要与可信 MIME；后续存储通过重新打开流读取。 */
    private Inspected inspect(MultipartFile file) {
        // 1.先校验空文件、原始名称和扩展名，阻断非法路径及超长元数据。
        if (file == null || file.isEmpty()) throw new UploadFailure("EMPTY_FILE", "上传文件不能为空");
        var name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 255 || name.codePoints().anyMatch(Character::isISOControl)
                || name.contains("/") || name.contains("\\")) {
            throw new UploadFailure("INVALID_NAME", "文件名不合法或超过255个字符");
        }
        var ext = FilePathUtils.getExtension(name);
        if (ext.length() > 20 || (!ext.isEmpty() && !ext.matches("[a-z0-9_-]+"))) {
            throw new UploadFailure("INVALID_EXTENSION", "扩展名不合法或超过20个字符");
        }
        // 2.按声明大小提前拒绝超限文件，再根据内容签名确定 MIME。
        if (file.getSize() > MAX_FILE_SIZE) throw new UploadFailure("FILE_TOO_LARGE", "文件不能超过50MB");
        try (var input = new BufferedInputStream(file.getInputStream())) {
            var type = FileContentUtils.detectContentType(input);
            var digest = MessageDigest.getInstance("SHA-256");
            var buffer = new byte[64 * 1024];
            long size = 0;
            // 3.边读取边累计实际大小和摘要，声明大小不能代替实际字节检查。
            for (int read; (read = input.read(buffer)) != -1;) {
                size += read;
                if (size > MAX_FILE_SIZE) throw new UploadFailure("FILE_TOO_LARGE", "文件不能超过50MB");
                digest.update(buffer, 0, read);
            }
            if (size == 0) throw new UploadFailure("EMPTY_FILE", "上传文件不能为空");
            return new Inspected(name, ext, size, type, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) {
            throw new UploadFailure("FILE_READ_FAILED", "读取上传文件失败");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 使用全新日期 UUID key 写入字节；失败或并发去重遗留的 key 由孤儿清理回收。 */
    private String write(MultipartFile file, Inspected inspected) {
        // 1.每次写入生成新 key，避免迟到的旧对象删除影响新对象。
        var key = FilePathUtils.generatePath(inspected.name());
        // 2.重新打开 multipart 流，在数据库事务外执行存储操作。
        try (var input = file.getInputStream()) {
            storage.upload(input, key, inspected.contentType(), inspected.size());
            return key;
        } catch (IOException | RuntimeException e) {
            log.error("文件存储写入失败", e);
            throw new UploadFailure("STORAGE_FAILED", "存储写入失败，请重试");
        }
    }

    /** 使用单调时钟拒绝超过本次孤儿保留期的上传，降低元数据引用已清理字节的风险。 */
    private void checkElapsed(long started, Duration maximum) {
        // 接受进程暂停跨越孤儿保留期的边界；检查后到提交之间仍存在极小窗口。
        if (Duration.ofNanos(ticker.read() - started).compareTo(maximum) >= 0) {
            throw new UploadFailure("UPLOAD_EXPIRED", "上传耗时超过孤儿保留期，请重新上传");
        }
    }

    /** 校验文件状态与读取权限后，返回不含内部存储位置的文件详情。 */
    @Override
    public FileUploadVO detail(long id) {
        // 1.加载可用文件并校验上传者身份或对应管理权限。
        var file = this.require(id);
        this.authorize(file, "system:file:query");
        // 2.按固定寿命档位签发链接，只向接口调用方暴露逻辑文件信息。
        return new FileUploadVO(id, file.getOriginalName(), file.getFileExt(), file.getFileSize(),
                file.getContentType(), this.sign(file, "view", AccessLevel.PRIVATE));
    }

    /** 在当前权限与文件状态校验通过后，按浏览或下载用途签发短期访问链接。 */
    @Override
    public String url(long id, boolean download) {
        var file = this.require(id);
        this.authorize(file, download ? "system:file:download" : "system:file:query");
        return this.sign(file, download ? "download" : "view", AccessLevel.PRIVATE);
    }

    /** 整批校验后逻辑删除；真实业务引用必须先经对应业务模块解除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) { bindings.delete(ids); }

    /** 为已完成业务记录鉴权的响应批量解析文件；寿命档位仅控制链接有效时间。 */
    @Override
    public Map<Long, FileView> resolve(Set<Long> ids, AccessLevel level) {
        // 1.调用者已完成业务记录鉴权，批量加载头像等合法业务引用，不按上传者再次过滤。
        var result = new HashMap<Long, FileView>();
        for (var file : this.load(ids).values()) {
            // 2.逐项复查物理对象可用性和存储身份，再复用或签发链接。
            this.available(file);
            result.put(file.getId(), new FileView(file.getId(), file.getOriginalName(),
                    this.sign(file, "view", level), file.getFileExt(), file.getFileSize()));
        }
        return result;
    }

    /** 联表批量加载活跃逻辑文件及其对象元数据，避免响应序列化逐字段查询。 */
    private Map<Long, FileRecord> load(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var query = new MPJLambdaWrapper<SysFile>().selectAll(SysFile.class)
                .select(SysFileObject::getStorageId, SysFileObject::getStorageType, SysFileObject::getBucket,
                        SysFileObject::getObjectKey, SysFileObject::getFileSize, SysFileObject::getContentType,
                        SysFileObject::getStatus)
                .innerJoin(SysFileObject.class, SysFileObject::getId, SysFile::getObjectId)
                .in(SysFile::getId, ids);
        return files.selectJoinList(FileRecord.class, query).stream()
                .collect(Collectors.toMap(FileRecord::getId, Function.identity()));
    }

    /** 取得未删除且物理对象可用的逻辑文件。 */
    private FileRecord require(long id) {
        var file = this.load(Set.of(id)).get(id);
        if (file == null) throw new BusinessException("文件不存在或已删除");
        this.available(file);
        return file;
    }

    /** 校验对象状态和完整存储身份，禁止把历史 key 静默指向当前其他后端。 */
    private void available(FileRecord file) {
        if (!"AVAILABLE".equals(file.getStatus()) || !storage.getStorageId().equals(file.getStorageId())
                || storage.getStorageType() != file.getStorageType() || !storage.getBucket().equals(file.getBucket())) {
            throw new BusinessException("文件对象不可用或存储身份不一致");
        }
    }

    /** 通用文件接口仅允许上传者或拥有对应文件管理权限的用户访问。 */
    private void authorize(SysFile file, String permission) {
        var actor = SecurityUtils.getUserId();
        if (actor == null || (!actor.equals(file.getCreateBy()) && !permissions.has(permission))) {
            throw new AccessDeniedException("无权访问此文件");
        }
    }

    /**
     * 按文件 ID、用途和寿命档位缓存签名链接，剩余寿命不超过原寿命的 20% 时重新签发。
     * 调用方必须先完成本次权限和状态校验，命中缓存不能代替鉴权。
     */
    private String sign(FileRecord file, String purpose, AccessLevel level) {
        // 1.查找当前用途与寿命档位对应的缓存，检查剩余有效时间。
        var key = new UrlKey(file.getId(), purpose, level);
        var cached = urls.getIfPresent(key);
        var threshold = level.getDuration().dividedBy(5);
        if (cached != null && Duration.between(Instant.now(), cached.expiresAt()).compareTo(threshold) > 0) {
            return cached.url();
        }
        // 2.重新签发并更新缓存；S3 响应头无法在实际读取时缩短 max-age，因此采用 0。
        var url = storage.getAccessUrl(new StorageService.SignRequest(file.getId(), file.getObjectKey(),
                file.getOriginalName(), file.getContentType(), purpose, level.getDuration(), "private, max-age=0"));
        urls.put(key, new SignedUrl(url, Instant.now().plus(level.getDuration())));
        return url;
    }

    /** 校验 LOCAL 直链签名后输出文件，下载用途使用二进制附件，浏览用途遵循图片白名单。 */
    @Override
    public void access(long id, long exp, String purpose, String signature, HttpServletResponse response) {
        // 1.校验后端、用途、有效期及签名，签名同时覆盖文件 ID、到期时间和用途。
        if (storage.getStorageType() != StorageType.LOCAL || !Set.of("view", "download").contains(purpose)
                || Instant.now().getEpochSecond() >= exp
                || !new LocalAccessUrlSigner(properties.access().signSecret())
                        .verify(Long.toString(id), exp, purpose, signature)) {
            throw new AccessDeniedException("访问签名无效或已过期");
        }
        // 2.重新检查文件状态，设置原始名称及不超过链接剩余寿命的私有缓存。
        var file = this.require(id);
        var type = "download".equals(purpose) ? FileContentUtils.BINARY_TYPE : file.getContentType();
        response.setContentType(type);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, FileContentUtils.contentDisposition(type, file.getOriginalName()));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + Math.max(0, exp - Instant.now().getEpochSecond()));
        response.setContentLengthLong(file.getFileSize());
        // 3.流式输出字节，字节缺失或读取失败时记录逻辑文件与物理对象编号。
        try (var input = storage.download(file.getObjectKey())) {
            input.transferTo(response.getOutputStream());
        } catch (IOException | RuntimeException e) {
            log.error("文件字节缺失或读取失败: fileId={}, objectId={}", id, file.getObjectId(), e);
            throw new BusinessException("文件读取失败");
        }
    }

    private record Inspected(String name, String ext, long size, String contentType, String hash) { }
    private record UrlKey(long fileId, String purpose, AccessLevel level) { }
    private record SignedUrl(String url, Instant expiresAt) { }
    private static final class UploadFailure extends BusinessException {
        final String code;
        UploadFailure(String code, String message) { super(message); this.code = code; }
    }
}
