package com.ray.framework.web.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Scalar API 文档页面（springdoc 补丁）
 *
 * <p><b>上游缺陷</b>：springdoc 3.0.1（见 pom.xml 的 {@code springdoc.version}）的
 * {@code AbstractScalarController#getDocs} 在生成多分组文档源时，把每个分组的
 * {@code default} 硬编码为 {@code false}（main 分支截至 2026-11 仍未修复），
 * 导致 Scalar 打开时没有选中文档、页面空白。这里接管 {@code /scalar}，
 * 将“系统管理”分组标为默认文档。</p>
 *
 * <p><b>耦合</b>：starter 自带页面通过 {@code scalar.path=/scalar-starter} 挪到内部路径，
 * 仅用于提供 {@code scalar.js}；该值不能改成 {@code /scalar}，
 * 否则与本控制器的映射冲突、启动失败。</p>
 *
 * <p><b>移除条件</b>：升级 springdoc 后确认 {@code /scalar} 能直接展示“系统管理”分组，
 * 即可删除本类、把 {@code scalar.path} 改回默认值，并清理 SecurityConfig 中
 * {@code /scalar-starter/**} 的白名单。</p>
 */
@Controller
@RequestMapping("/scalar")
@ConditionalOnProperty(name = "ray.api-docs.enabled", havingValue = "true")
public class ScalarController {

    /** 默认选中的文档分组（OpenApiConfig 中定义的分组名），不存在时回退到第一个分组 */
    private static final String DEFAULT_GROUP = "系统管理";

    private final List<GroupedOpenApi> groupedApis;
    private final SpringDocConfigProperties springDocConfigProperties;
    private final String scalarPath;

    public ScalarController(
            List<GroupedOpenApi> groupedApis,
            SpringDocConfigProperties springDocConfigProperties,
            @Value("${scalar.path:/scalar}") String scalarPath) {
        this.groupedApis = groupedApis;
        this.springDocConfigProperties = springDocConfigProperties;
        this.scalarPath = scalarPath;
    }

    @GetMapping(value = {"", "/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> scalar(HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        String requestUri = request.getRequestURI();
        String baseUrl = requestUrl.substring(0, requestUrl.length() - requestUri.length())
                + request.getContextPath();
        String apiDocsPath = springDocConfigProperties.getApiDocs().getPath();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(render(baseUrl, scalarPath + "/scalar.js", apiDocsPath, groupedApis));
    }

    /**
     * 渲染 Scalar 页面，默认选中 {@link #DEFAULT_GROUP} 分组。
     *
     * @param baseUrl     服务基地址，如 http://localhost:9527
     * @param scalarJsUrl scalar.js 地址
     * @param apiDocsPath OpenAPI 文档基路径，如 /v3/api-docs
     * @param groups      文档分组
     */
    static String render(String baseUrl, String scalarJsUrl, String apiDocsPath, List<GroupedOpenApi> groups) {
        boolean hasDefault = groups.stream().anyMatch(g -> DEFAULT_GROUP.equals(g.getGroup()));
        StringBuilder sources = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            GroupedOpenApi group = groups.get(i);
            boolean isDefault = hasDefault ? DEFAULT_GROUP.equals(group.getGroup()) : i == 0;
            if (i > 0) {
                sources.append(',');
            }
            String title = group.getDisplayName() != null ? group.getDisplayName() : group.getGroup();
            sources.append("{\"url\":\"").append(escape(baseUrl)).append(escape(apiDocsPath))
                    .append('/').append(escape(group.getGroup()))
                    .append("\",\"title\":\"").append(escape(title))
                    .append("\",\"default\":").append(isDefault).append('}');
        }
        return """
                <!doctype html>
                <html>
                  <head>
                    <title>Scalar API Reference</title>
                    <meta charset="utf-8" />
                    <meta
                      content="width=device-width, initial-scale=1"
                      name="viewport" />
                  </head>

                  <body>
                    <div id="app"></div>

                    <!-- Load the Script -->
                    <script src="%s"></script>

                    <!-- Initialize the Scalar API Reference -->
                    <script>
                      Scalar.createApiReference('#app', {"showSidebar":true,"hideModels":false,"hideTestRequestButton":false,"darkMode":false,"hideDarkModeToggle":false,"withDefaultFonts":true,"defaultOpenAllTags":false,"expandAllModelSections":false,"expandAllResponses":false,"hideSearch":false,"theme":"default","layout":"modern","favicon":"favicon.svg","hideClientButton":false,"sources":[%s],"persistAuth":false,"telemetry":true,"documentDownloadType":"both","orderRequiredPropertiesFirst":true,"showOperationId":false})
                    </script>
                  </body>
                </html>
                """.formatted(escape(scalarJsUrl), sources);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
