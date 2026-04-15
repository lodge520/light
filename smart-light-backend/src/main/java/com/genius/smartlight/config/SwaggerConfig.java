package com.genius.smartlight.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI smartLightOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Light Backend API")
                        .description("智能服装射灯系统后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Hao Lee")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/lodge520/light"));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-device")
                .pathsToMatch("/admin/device/**")
                .build();
    }
}