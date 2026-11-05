package com.ray.framework.web.translate;

import com.ray.common.core.translate.EnableTranslate;
import com.ray.framework.web.jackson.TranslateBeanPropertyWriter;
import com.ray.common.storage.jackson.StorageFileSerializer;
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
 * 翻译与文件预扫描、批量查询，并通过 {@link ObjectWriter#withAttribute} 把结果
 * 注入到 Jackson 序列化上下文，由
 * {@link com.ray.framework.web.jackson.TranslateSerializerModifier} 注入的
 * 虚拟 BeanPropertyWriter 读取。
 * <p>
 * 使用 Java 25 {@link ScopedValue} 在 writeInternal 与 customizeWriter 之间传递响应缓存，
 * 避免 ThreadLocal 的显式清理与虚拟线程下的内存漏洞。
 */
public class TranslateAwareJsonHttpMessageConverter extends JacksonJsonHttpMessageConverter {

    private static final ScopedValue<TranslateCollector.ResponseCache> CURRENT_CACHE = ScopedValue.newInstance();

    private final TranslateCollector collector;

    public TranslateAwareJsonHttpMessageConverter(JsonMapper jsonMapper, TranslateCollector collector) {
        super(jsonMapper);
        this.collector = collector;
    }

    /** 写出响应前完成批量解析，在本次写出作用域中传递缓存，结束后自动解除绑定。 */
    @Override
    protected void writeInternal(Object object, ResolvableType resolvableType,
                                 HttpOutputMessage outputMessage,
                                 @Nullable Map<String, Object> hints)
            throws IOException, HttpMessageNotWritableException {
        // 1.文件字段始终参与扫描，普通翻译字段由当前接口的 EnableTranslate 开关控制。
        var cache = collector.collectResponse(object, this.isTranslateEnabled());
        if (cache.translations().isEmpty() && cache.files().isEmpty()) {
            super.writeInternal(object, resolvableType, outputMessage, hints);
            return;
        }
        // 2.有解析结果时绑定当前响应缓存，避免跨请求或虚拟线程复用缓存数据。
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

    /** 将当前响应的翻译和文件缓存注入 Jackson 上下文，供字段写出器直接读取。 */
    @Override
    protected ObjectWriter customizeWriter(ObjectWriter writer,
                                           @Nullable JavaType javaType,
                                           @Nullable MediaType contentType) {
        ObjectWriter base = super.customizeWriter(writer, javaType, contentType);
        if (CURRENT_CACHE.isBound()) {
            var cache = CURRENT_CACHE.get();
            return base.withAttribute(TranslateBeanPropertyWriter.TRANSLATE_CACHE_ATTR, cache.translations())
                    .withAttribute(StorageFileSerializer.CACHE_ATTRIBUTE, cache.files());
        }
        return base;
    }

    /** 按当前实际 HandlerMethod 的方法或 Controller 类注解决定是否启用字段翻译。 */
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
