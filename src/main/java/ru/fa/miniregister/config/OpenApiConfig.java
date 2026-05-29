package ru.fa.miniregister.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI / Swagger UI.
 * Документация доступна по адресу /swagger-ui.html после запуска сервера.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "MiniRegister API — Бронь переговорной",
                version     = "1.0",
                description = "REST API для управления бронями переговорных комнат (B-records). "
                            + "Поддерживает CRUD-операции и фоновый импорт/экспорт CSV.",
                contact     = @Contact(name = "MiniRegister")
        ),
        tags = {
                @Tag(name = "records", description = "CRUD-операции над бронями"),
                @Tag(name = "jobs",    description = "Фоновые задания импорта/экспорта CSV")
        }
)
public class OpenApiConfig {
}
