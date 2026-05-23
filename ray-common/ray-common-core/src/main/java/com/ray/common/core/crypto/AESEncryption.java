package com.ray.common.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM 加密工具
 * <p>
 * GCM 模式提供了认证加密，安全性高于 CBC
 */
public class AESEncryption {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int NONCE_LENGTH_BYTE = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public AESEncryption(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        if (decodedKey.length != 32) {
            throw new IllegalArgumentException("密钥长度必须为 32 字节（256 位），当前长度：" + decodedKey.length);
        }
        this.key = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * 加密
     *
     * @param plaintext 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] nonce = new byte[NONCE_LENGTH_BYTE];
            RANDOM.nextBytes(nonce);

            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 nonce 和 ciphertext，然后 Base64 编码
            byte[] result = new byte[NONCE_LENGTH_BYTE + ciphertext.length];
            System.arraycopy(nonce, 0, result, 0, NONCE_LENGTH_BYTE);
            System.arraycopy(ciphertext, 0, result, NONCE_LENGTH_BYTE, ciphertext.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密
     *
     * @param encryptedText Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedText);

            if (data.length < NONCE_LENGTH_BYTE) {
                throw new IllegalArgumentException("密文长度不足");
            }

            byte[] nonce = new byte[NONCE_LENGTH_BYTE];
            byte[] ciphertext = new byte[data.length - NONCE_LENGTH_BYTE];

            System.arraycopy(data, 0, nonce, 0, NONCE_LENGTH_BYTE);
            System.arraycopy(data, NONCE_LENGTH_BYTE, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }
}
