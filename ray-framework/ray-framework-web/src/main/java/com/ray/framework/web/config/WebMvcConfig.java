package com.ray.framework.web.config;

import com.ray.framework.web.translate.TranslateAwareJsonHttpMessageConverter;
import com.ray.framework.web.translate.TranslateCollector;
import org.springframework.beans.factory.annotation.Value;
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
     * 允许跨域的前端来源，逗号分隔。用 allowedOrigins（精确匹配）而不用 "*"，
     * 因为 allowCredentials(true) 与通配符组合等于任意站点可携带 Cookie 调用接口。
     * 本地默认放行常见 Vite 端口，生产通过环境变量 RAY_CORS_ALLOWED_ORIGINS 覆盖。
     */
    @Value("${ray.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

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
     * 跨域配置（生产务必通过 RAY_CORS_ALLOWED_ORIGINS 收敛到可信前端域名）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("X-Request-Id")
                .maxAge(3600);
    }
}
