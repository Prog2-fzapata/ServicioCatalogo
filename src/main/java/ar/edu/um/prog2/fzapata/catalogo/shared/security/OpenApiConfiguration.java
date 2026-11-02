package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import java.util.List;
import java.util.Objects;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {

    static final String BEARER_SCHEME = "bearer-jwt";

    // Declara el esquema Bearer JWT (Swagger UI muestra el botón «Authorize») y lo exige en todo
    // menos en los endpoints públicos, que se marcan sin requisito de seguridad
    @Bean
    OpenApiCustomizer bearerJwtCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(BEARER_SCHEME,
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
            openApi.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
            SecurityConfiguration.PUBLIC_API_PATHS.stream()
                    .map(path -> openApi.getPaths() == null ? null : openApi.getPaths().get(path))
                    .filter(Objects::nonNull)
                    .forEach(item -> item.readOperations().forEach(operation -> operation.setSecurity(List.of())));
        };
    }
}
