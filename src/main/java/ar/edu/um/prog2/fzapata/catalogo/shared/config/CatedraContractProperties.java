package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Nombres del contrato de la cátedra (rutas y claves). Viven en el .env y no en el código: si la cátedra
// cambia uno, se edita la configuración y no hace falta recompilar
@Validated
@ConfigurationProperties(prefix = "catedra.contract")
public record CatedraContractProperties(
        // Ruta REST del snapshot (variable CATEDRA_SNAPSHOT_PATH): sin la "/" inicial el pedido saldría mal formado
        @NotBlank @Pattern(regexp = "/.+", message = "debe empezar con / y tener algo después") String snapshotPath,
        @Valid @NotNull RedisKeys redisKeys) {

    // Claves de lectura de la cátedra en Redis (anexo §14)
    public record RedisKeys(
            @NotBlank String metadata,
            // La clave de cada versión es este prefijo seguido del número de versión
            @NotBlank String changesPrefix,
            @NotBlank String categories,
            @NotBlank String professionals,
            @NotBlank String weeklySchedules) {
    }
}
