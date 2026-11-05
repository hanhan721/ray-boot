package com.ray.common.storage.support;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** 将规范化物理位置转换为稳定存储身份，避免凭证或公开域名变化影响历史对象。 */
public final class StorageIdentity {
    private StorageIdentity() { }

    /** 对后端类型与物理位置使用固定 UTF-8 编码计算 SHA-256 小写十六进制指纹。 */
    public static String fingerprint(String type, String location) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((type + "\n" + location).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 规范化内部 endpoint 的主机、默认端口和尾部斜杠，排除用户信息与查询参数。 */
    public static String endpoint(String endpoint) {
        // 1.解析并规范化 URI，拒绝不能唯一定位物理后端的配置形式。
        var uri = URI.create(endpoint).normalize();
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !java.util.Set.of("http", "https").contains(uri.getScheme())) {
            throw new IllegalArgumentException("非法存储 endpoint");
        }
        // 2.显式补全默认端口并统一主机大小写，使等价地址得到相同指纹输入。
        int port = uri.getPort();
        if (port == -1) port = "https".equals(uri.getScheme()) ? 443 : 80;
        return uri.getScheme() + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port
                + uri.getPath().replaceAll("/+$", "");
    }
}
