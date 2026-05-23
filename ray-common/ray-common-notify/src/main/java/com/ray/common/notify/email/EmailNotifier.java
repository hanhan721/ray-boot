package com.ray.common.notify.email;

import com.ray.common.notify.model.NotifyChannel;
import com.ray.common.notify.model.NotifyRequest;
import com.ray.common.notify.spi.Notifier;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Email 渠道通知实现（SMTP）
 * <p>
 * 仅在存在 {@link MailFromProvider} Bean 时激活。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MailFromProvider.class)
public class EmailNotifier implements Notifier {

    private final MailFromProvider mailFromProvider;

    @Override
    public boolean supports(NotifyChannel channel) {
        return NotifyChannel.EMAIL == channel;
    }

    @Override
    public void send(NotifyRequest request) {
        var email = buildEmailRequest(request);
        try {
            var mailSender = mailFromProvider.getMailSender();
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFromProvider.getFrom());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.content(), email.html());
            mailSender.send(message);
            log.info("[Notify] 邮件发送成功: to={}, template={}", email.to(), request.templateId());
        } catch (MessagingException e) {
            log.error("[Notify] 邮件发送失败: to={}, template={}", email.to(), request.templateId(), e);
            throw new RuntimeException("邮件发送失败，请稍后重试", e);
        }
    }

    private EmailNotifyRequest buildEmailRequest(NotifyRequest request) {
        var params = request.params();
        var subject = params != null && StringUtils.hasText((String) params.get("subject"))
                ? (String) params.get("subject")
                : request.templateId();
        var content = params != null && params.get("content") != null
                ? String.valueOf(params.get("content"))
                : "";
        var html = params != null && "true".equalsIgnoreCase(String.valueOf(params.get("html")));
        return new EmailNotifyRequest(request.target(), subject, content, html);
    }
}
