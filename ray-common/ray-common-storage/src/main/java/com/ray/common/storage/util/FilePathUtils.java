package com.ray.common.storage.util;

import org.springframework.util.StringUtils;

import java.net.URLConnection;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 文件路径工具类
 */
public final class FilePathUtils {

    private FilePathUtils() {
    }

    /**
     * 生成存储路径：{year}/{month}/{day}/{uuid}.{ext}
     *
     * @param originalFilename 原始文件名
     * @return 存储路径
     */
    public static String generatePath(String originalFilename) {
        var ext = getExtension(originalFilename);
        var uuid = UUID.randomUUID().toString().replace("-", "");
        var today = LocalDate.now();
        var fileName = StringUtils.hasText(ext) ? uuid + "." + ext : uuid;
        return "%d/%02d/%02d/%s".formatted(today.getYear(), today.getMonthValue(), today.getDayOfMonth(), fileName);
    }

    /**
     * 获取文件扩展名（不含点号）
     *
     * @param filename 文件名
     * @return 扩展名，无扩展名返回空字符串
     */
    public static String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        var dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 从存储路径中提取文件名
     *
     * @param filePath 存储路径
     * @return 文件名
     */
    public static String getFileName(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return "";
        }
        var slashIndex = filePath.lastIndexOf('/');
        return slashIndex < 0 ? filePath : filePath.substring(slashIndex + 1);
    }

    /**
     * 根据文件名推断 MIME 类型
     *
     * @param filename 文件名
     * @return MIME 类型，未知返回 application/octet-stream
     */
    public static String getContentType(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "application/octet-stream";
        }
        var contentType = URLConnection.guessContentTypeFromName(filename);
        return contentType != null ? contentType : "application/octet-stream";
    }
}
