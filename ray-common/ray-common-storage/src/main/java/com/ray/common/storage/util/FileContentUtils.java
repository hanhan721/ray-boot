package com.ray.common.storage.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/** 按文件签名确定浏览器可展示的栅格图片类型，其余内容作为二进制附件。 */
public final class FileContentUtils {

    public static final String BINARY_TYPE = "application/octet-stream";
    public static final int SIGNATURE_LENGTH = 12;
    private static final Set<String> INLINE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private FileContentUtils() { }

    public static String detectContentType(BufferedInputStream input) throws IOException {
        input.mark(SIGNATURE_LENGTH);
        var signature = input.readNBytes(SIGNATURE_LENGTH);
        input.reset();
        return detectContentType(signature);
    }

    public static String detectContentType(byte[] signature) {
        if (startsWith(signature, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10})) {
            return "image/png";
        }
        if (startsWith(signature, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
            return "image/jpeg";
        }
        if (startsWith(signature, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(signature, "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
            return "image/gif";
        }
        if (signature.length >= 12 && startsWith(signature, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && Arrays.equals(signature, 8, 12, "WEBP".getBytes(StandardCharsets.US_ASCII), 0, 4)) {
            return "image/webp";
        }
        return BINARY_TYPE;
    }

    public static String contentDisposition(String contentType, String filename) {
        var disposition = INLINE_TYPES.contains(contentType) ? "inline" : "attachment";
        var encoded = URLEncoder.encode(filename == null ? "file" : filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return disposition + "; filename*=UTF-8''" + encoded;
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && Arrays.equals(value, 0, prefix.length, prefix, 0, prefix.length);
    }
}
