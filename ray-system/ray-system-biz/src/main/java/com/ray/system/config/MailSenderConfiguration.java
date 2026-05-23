package com.ray.system.config;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.notify.email.MailFromProvider;
import com.ray.system.event.ParamChangedEvent;
import com.ray.system.service.ParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * 邮件发送配置
 * <p>
 * 从 sys_param 表读取 SMTP 配置，缓存 {@link JavaMailSender} 实例。
 * 当 mail.* 参数变更时自动重建。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MailSenderConfiguration {

    private static final String MAIL_PARAM_PREFIX = "mail.";

    private final ParamService paramService;
    private volatile JavaMailSenderImpl cachedSender;

    @Bean
    public MailFromProvider mailFromProvider() {
        return new MailFromProvider() {
            @Override
            public String getFrom() {
                return paramService.getStringByKey("mail.from");
            }

            @Override
            public JavaMailSender getMailSender() {
                if (cachedSender == null) {
                    synchronized (MailSenderConfiguration.this) {
                        if (cachedSender == null) {
                            cachedSender = buildMailSender();
                        }
                    }
                }
                return cachedSender;
            }
        };
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParamChanged(ParamChangedEvent event) {
        if (event.paramKey().startsWith(MAIL_PARAM_PREFIX)) {
            synchronized (this) {
                cachedSender = null;
            }
            log.info("[Mail] 邮件参数 [{}] 已变更，JavaMailSender 将在下次发送时重建", event.paramKey());
        }
    }

    private JavaMailSenderImpl buildMailSender() {
        var host = requireParam("mail.host");
        var port = paramService.getIntegerByKey("mail.port");
        if (port == null) {
            throw new BusinessException("邮件参数 [mail.port] 未配置");
        }
        var username = requireParam("mail.username");
        var password = requireParam("mail.password");

        var sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        var props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        log.info("[Mail] JavaMailSender 已构建, host={}", host);
        return sender;
    }

    private String requireParam(String key) {
        var value = paramService.getStringByKey(key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("邮件参数 [" + key + "] 未配置");
        }
        return value;
    }
}
