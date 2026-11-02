package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

// Verificación de versión: cada cuánto compara el servicio su copia con la de la cátedra. Los valores salen de
// variables de entorno del docker-compose.yml (CATEDRA_SYNC_*), sin valor por defecto en el código
@Validated
@ConfigurationProperties(prefix = "catedra.sync")
public record CatedraSyncProperties(
        // Espera entre el fin de una verificación y el comienzo de la siguiente
        @NotNull @DurationMin(nanos = 1) Duration checkInterval,
        // Espera antes de la primera, después de arrancar: con la base vacía esa vuelta trae el snapshot
        @NotNull @DurationMin(nanos = 0) Duration initialDelay) {
}
