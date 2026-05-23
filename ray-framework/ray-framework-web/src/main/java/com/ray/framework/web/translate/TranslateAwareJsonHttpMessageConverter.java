package com.ray.framework.web.translate;

import com.ray.common.core.translate.EnableTranslate;
import com.ray.common.core.translate.TranslateKey;
import com.ray.framework.web.jackson.TranslateBeanPropertyWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

/**
 * 替换默认 {@link JacksonJsonHttpMessageConverter}：序列化前做一次
 * 翻译预扫描与批量查询，并通过 {@link ObjectWriter#withAttribute} 把结果
 * 注入到 Jackson 序列化上下文，由
 * {@link com.ray.framework.web.jackson.TranslateSerializerModifier} 注入的
 * 虚拟 BeanPropertyWriter 读取。
 * <p>
 * 使用 Java 25 {@link ScopedValue} 在 writeInternal 与 customizeWriter 之间传递翻译缓存，
 * 避免 ThreadLocal 的显式清理与虚拟线程下的内存漏洞。
 */
public class TranslateAwareJsonHttpMessageConverter extends JacksonJsonHttpMessageConverter {

    private static final ScopedValue<Map<TranslateKey, String>> CURRENT_CACHE = ScopedValue.newInstance();

    private final TranslateCollector collector;

    public TranslateAwareJsonHttpMessageConverter(JsonMapper jsonMapper, TranslateCollector collector) {
        super(jsonMapper);
        this.collector = collector;
    }

    @Override
    protected void writeInternal(Object object, ResolvableType resolvableType,
                                 HttpOutputMessage outputMessage,
                                 @Nullable Map<String, Object> hints)
            throws IOException, HttpMessageNotWritableException {
        if (!isTranslateEnabled()) {
            super.writeInternal(object, resolvableType, outputMessage, hints);
            return;
        }
        Map<TranslateKey, String> cache = collector.collect(object);
        if (cache.isEmpty()) {
            super.writeInternal(object, resolvableType, outputMessage, hints);
            return;
        }
        try {
            ScopedValue.where(CURRENT_CACHE, cache).call(() -> {
                super.writeInternal(object, resolvableType, outputMessage, hints);
                return null;
            });
        } catch (IOException | HttpMessageNotWritableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new HttpMessageNotWritableException("Translate-aware write failed: " + t.getMessage(), t);
        }
    }

    @Override
    protected ObjectWriter customizeWriter(ObjectWriter writer,
                                           @Nullable JavaType javaType,
                                           @Nullable MediaType contentType) {
        ObjectWriter base = super.customizeWriter(writer, javaType, contentType);
        if (CURRENT_CACHE.isBound()) {
            Map<TranslateKey, String> cache = CURRENT_CACHE.get();
            if (!cache.isEmpty()) {
                return base.withAttribute(TranslateBeanPropertyWriter.TRANSLATE_CACHE_ATTR, cache);
            }
        }
        return base;
    }

    private boolean isTranslateEnabled() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), EnableTranslate.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), EnableTranslate.class);
    }
}
