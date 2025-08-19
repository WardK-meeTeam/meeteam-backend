package com.wardk.meeteam_backend.global.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.*;


@OpenAPIDefinition(
        info = @Info(
                title = "Meeteam",
                description = """
                                Api Docs
                        """,
                version = "1.0v"
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "로컬 서버")
        }

)
@Configuration
public class SwaggerConfig {


    @Bean
    public OpenAPI openAPI() {
        SecurityScheme apiKey = new SecurityScheme()
                .type(HTTP)
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Token");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("Bearer Token", apiKey))
                .addSecurityItem(securityRequirement)
                .servers(List.of(
                                new io.swagger.v3.oas.models.servers.Server()
                                        .url("http://localhost:8080")
                                        .description("로컬 서버"),
                                new io.swagger.v3.oas.models.servers.Server()
                                        .url("http://3.37.43.118:8080")
                                        .description("EC2 서버")

                        )
                );
    }
}
