package com.ray.common.storage.support;

import com.ray.common.core.exception.BusinessException;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * LOCAL 访问链接签名器
 */
public class LocalAccessUrlSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final String secret;

    public LocalAccessUrlSigner(String secret) {
        this.secret = secret;
    }

    public String sign(String path, long expiresAt, String level) {
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException("未配置存储访问签名密钥");
        }
        try {
            var content = path + "\n" + expiresAt + "\n" + level;
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new BusinessException("生成访问签名失败");
        }
    }

    public boolean verify(String path, long expiresAt, String level, String signature) {
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        String expected = sign(path, expiresAt, level);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String toHex(byte[] bytes) {
        var builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
