package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

// Costo del hash de contraseñas (Argon2id). Los mínimos son los de OWASP: nadie puede debilitar el hash por error
@Validated
@ConfigurationProperties(prefix = "password.argon2")
public record Argon2Properties(
        // Memoria por hash en KiB (variable PASSWORD_ARGON2_MEMORY_KIB): 19456 KiB son 19 MiB
        @Min(value = 19456, message = "no puede bajar del mínimo de OWASP ({value} KiB)") int memoryKib,
        // Pasadas sobre esa memoria (variable PASSWORD_ARGON2_ITERATIONS)
        @Min(value = 2, message = "no puede bajar del mínimo de OWASP ({value})") int iterations,
        // Hilos por hash (variable PASSWORD_ARGON2_PARALLELISM)
        @Min(value = 1, message = "debe ser al menos {value}") int parallelism) {
}
