package com.ray.framework.web.config;

import com.ray.common.security.evaluator.PermissionEvaluator;
import com.ray.framework.web.jackson.FieldPermissionSerializerModifier;
import com.ray.framework.web.jackson.TranslateSerializerModifier;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.module.SimpleModule;

import java.util.TimeZone;

/**
 * Jackson 配置
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperCustomizer(BeanFactory beanFactory,
                                                            AutowireCapableBeanFactory autowireBeanFactory,
                                                            PermissionEvaluator permissionEvaluator) {
        return builder -> {
            builder.defaultTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                    .handlerInstantiator(new BeanLookupHandlerInstantiator(beanFactory, autowireBeanFactory));

            var fieldPermModule = new SimpleModule("FieldPermission");
            fieldPermModule.setSerializerModifier(new FieldPermissionSerializerModifier(permissionEvaluator));
            builder.addModule(fieldPermModule);

            var translateModule = new SimpleModule("Translate");
            translateModule.setSerializerModifier(new TranslateSerializerModifier(permissionEvaluator));
            builder.addModule(translateModule);
        };
    }

    /**
     * 优先从 Spring 容器查找已注册的 Bean 实例，找不到时 fallback 到 createBean。
     * <p>
     * Spring Framework 7 的 {@code JacksonHandlerInstantiator} 只用 {@code createBean}（无自动装配），
     * 导致通过 {@code @JsonSerialize(using=...)} 指定的序列化器无法获取 Spring 依赖。
     * 此实现改为 {@code getBean} 优先，使 {@code @Bean} 注册的序列化器实例能被 Jackson 正确使用。
     */
    private static class BeanLookupHandlerInstantiator extends HandlerInstantiator {

        private final BeanFactory beanFactory;
        private final AutowireCapableBeanFactory autowireBeanFactory;

        BeanLookupHandlerInstantiator(BeanFactory beanFactory, AutowireCapableBeanFactory autowireBeanFactory) {
            this.beanFactory = beanFactory;
            this.autowireBeanFactory = autowireBeanFactory;
        }

        @Override
        public ValueSerializer<?> serializerInstance(SerializationConfig config,
                                                     Annotated annotated, Class<?> serClass) {
            return (ValueSerializer<?>) resolve(serClass);
        }

        @Override
        public ValueDeserializer<?> deserializerInstance(DeserializationConfig config,
                                                         Annotated annotated, Class<?> deserClass) {
            return (ValueDeserializer<?>) resolve(deserClass);
        }

        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config,
                                                       Annotated annotated, Class<?> keyDeserClass) {
            return (KeyDeserializer) resolve(keyDeserClass);
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config,
                                                                  Annotated annotated, Class<?> builderClass) {
            return (TypeResolverBuilder<?>) resolve(builderClass);
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
                                                     Annotated annotated, Class<?> resolverClass) {
            return (TypeIdResolver) resolve(resolverClass);
        }

        private Object resolve(Class<?> clazz) {
            try {
                return beanFactory.getBean(clazz);
            } catch (NoSuchBeanDefinitionException e) {
                return autowireBeanFactory.createBean(clazz);
            }
        }
    }
}
