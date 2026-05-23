package com.ray.common.notify.email;

/**
 * Email 渠道请求模型
 * <p>
 * 调用方通过 {@link com.ray.common.notify.model.NotifyRequest#params()} 传入以下约定 key：
 * <ul>
 *   <li>{@code subject} — 邮件主题（缺省时使用 templateId）</li>
 *   <li>{@code content} — 邮件正文</li>
 *   <li>{@code html}    — 是否以 HTML 格式发送，值为 {@code "true"} 时生效（缺省为纯文本）</li>
 * </ul>
 *
 * @param to      收件人地址
 * @param subject 邮件主题
 * @param content 邮件正文
 * @param html    是否为 HTML 内容
 */
public record EmailNotifyRequest(
        String to,
        String subject,
        String content,
        boolean html
) {}
