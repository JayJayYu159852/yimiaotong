package cn.liyu.hospital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 4.2.0 接口文档 — 全局标题
 * <p>
 * 分组已迁移至 application.yml → springdoc.group-configs（springdoc 2.x 推荐方式，避免 GroupedOpenApi 废弃兼容问题）
 * <p>
 * 全局 Token 设置：打开 doc.html → 底部「文档管理」→「全局参数设置」→ 添加参数：
 * <pre>
 *   参数名称: Authorization
 *   参数值:   Bearer {你的Token}
 *   参数类型: header
 * </pre>
 *
 * @author 医秒通
 */

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI hospitalOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("医秒通 · 智慧医院挂号平台").version("2.0"));
    }
}
