package com.ray.framework.web.config;

import com.ray.framework.web.translate.TranslateAwareJsonHttpMessageConverter;
import com.ray.framework.web.translate.TranslateCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 替换默认的 {@link org.springframework.http.converter.json.JacksonJsonHttpMessageConverter}，
     * 在启用翻译的接口序列化前做翻译预扫描与批量查询。
     */
    @Bean
    public TranslateAwareJsonHttpMessageConverter translateAwareJsonHttpMessageConverter(
            JsonMapper jsonMapper, TranslateCollector translateCollector) {
        return new TranslateAwareJsonHttpMessageConverter(jsonMapper, translateCollector);
    }

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("X-Request-Id")
                .maxAge(3600);
    }
}
