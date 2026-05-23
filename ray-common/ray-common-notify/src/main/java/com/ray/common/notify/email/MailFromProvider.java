package com.ray.common.notify.email;

import org.springframework.mail.javamail.JavaMailSender;

/**
 * 邮件发送配置提供者
 * <p>
 * 由上层模块实现，从数据库或其他来源动态获取邮件配置。
 * 每次调用都读取最新配置，支持运行时动态修改 SMTP 参数。
 */
public interface MailFromProvider {

    /**
     * 获取发件人地址（含显示名，如 "Ray &lt;noreply@example.com&gt;"）
     */
    String getFrom();

    /**
     * 获取动态构建的 {@link JavaMailSender}，每次调用读取最新 SMTP 配置
     */
    JavaMailSender getMailSender();
}
