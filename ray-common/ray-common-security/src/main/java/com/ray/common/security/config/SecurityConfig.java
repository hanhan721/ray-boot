package com.ray.common.security.config;

import com.ray.common.security.filter.TokenAuthenticationFilter;
import com.ray.common.security.handler.AccessDeniedHandlerImpl;
import com.ray.common.security.handler.AuthenticationEntryPointImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // 无状态 Token 认证，保持 CSRF 禁用；CORS 交给 WebMvcConfig 的精确域名白名单
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Actuator 健康检查（容器健康检查依赖此端点）
                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()
                        // Scalar UI / OpenAPI
                        .requestMatchers(
                                "/scalar",
                                "/scalar/**",
                                "/v3/api-docs/**",
                                "/favicon.ico"
                        ).permitAll()
                        // 认证接口
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/sys/file/access"
                        ).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
