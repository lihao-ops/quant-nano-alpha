package com.hao.datacollector.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API 文档配置
 * 量化交易数据收集器服务 API 文档
 * 访问地址：http://localhost:8001/data-collector/doc.html
 */

@OpenAPIDefinition(
        info = @Info(
                title = "量化交易数据收集器 API",
                description = "量化交易数据服务模块：实现数据采集(包括定时拉取第三方数据)、清洗、存储等任务的 RESTful API 接口文档",
                version = "1.0.0",
                contact = @Contact(
                        name = "量化交易开发团队",
                        email = "quant-dev@example.com",
                        url = "https://github.com/your-org/datacollector"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
        ),
        tags = {
                @Tag(name = "数据采集", description = "股票、期货、基金等金融数据采集接口"),
                @Tag(name = "系统管理", description = "系统监控、健康检查和配置管理接口")
        }
)
@Configuration
public class OpenApiConfig {

    /**
     * 全部接口分组
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("全部接口")
                .packagesToScan("com.hao.datacollector.web.controller")
                .build();
    }

    /**
     * 数据采集模块分组
     */
    @Bean
    public GroupedOpenApi dataCollectionApi() {
        return GroupedOpenApi.builder()
                .group("data-collection")
                .displayName("数据采集模块")
                .pathsToMatch("/data_collection/**")
                .build();
    }

    /**
     * 系统管理模块分组
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("system")
                .displayName("系统管理模块")
                .pathsToMatch("/health/**")
                .build();
    }
}