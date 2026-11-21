package com.distrilink.accesscontrol.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Agrega el botón "Authorize" en Swagger: se pega el accessToken del login y Swagger
 * lo envía en el header "Authorization: Bearer ..." en cada petición protegida.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "DistriLink · access-control-service", version = "v1.0"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
