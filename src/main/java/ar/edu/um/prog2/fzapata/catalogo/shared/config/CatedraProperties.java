package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(
        // URL base de la API de la cátedra, sin "/" final (variable CATEDRA_BASE_URL)
        @NotBlank String baseUrl,
        // JWT técnico (variable CATEDRA_JWT): nunca se escribe en el código, en logs ni en mensajes de error
        @NotBlank String jwt,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    // El toString de un record imprime todos los campos: se oculta el JWT para que no llegue a ningún log
    @Override
    public String toString() {
        return "CatedraProperties[baseUrl=" + baseUrl + ", jwt=****, connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout + "]";
    }
}
