package com.ray.system.crypto;

import com.ray.common.core.crypto.AESEncryption;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * 参数加密解密辅助类
 * <p>
 * 管理参数的加密和解密操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParamEncryptionHelper {

    @Value("${crypto.aes-key:U7vE2w9pL4d6H8kJfQ1tB3sM5nC7xZ9aR2wG4eD6vF8hL0jN2kP4mS6oT8uV0xW}")
    private String aesKeyBase64;

    private AESEncryption encryption;

    /**
     * 初始化加密器
     */
    @PostConstruct
    public void initEncryption() {
        try {
            this.encryption = new AESEncryption(aesKeyBase64);
            log.info("参数加密器初始化成功");
        } catch (Exception e) {
            log.error("参数加密器初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("参数加密器初始化失败", e);
        }
    }

    /**
     * 加密参数值
     *
     * @param plaintext 明文
     * @return 密文
     */
    public String encrypt(String plaintext) {
        if (encryption == null) {
            throw new RuntimeException("加密器未初始化");
        }
        try {
            return encryption.encrypt(plaintext);
        } catch (Exception e) {
            log.error("参数加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("参数加密失败", e);
        }
    }

    /**
     * 解密参数值
     *
     * @param ciphertext 密文
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (encryption == null) {
            throw new RuntimeException("加密器未初始化");
        }
        try {
            return encryption.decrypt(ciphertext);
        } catch (Exception e) {
            log.error("参数解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("参数解密失败", e);
        }
    }

    /**
     * 尝试解密，失败时返回原值
     *
     * @param value 可能是密文的值
     * @return 解密后的值或原值
     */
    public String tryDecrypt(String value) {
        if (encryption == null) {
            log.warn("加密器未初始化，返回原值");
            return value;
        }
        try {
            return encryption.decrypt(value);
        } catch (Exception e) {
            log.debug("参数解密失败，返回原值: {}", e.getMessage());
            return value;
        }
    }
}
