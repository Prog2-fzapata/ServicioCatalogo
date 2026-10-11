package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        // Clave HMAC en Base64 (variable JWT_SECRET); turnos usa el mismo valor para validar
        @NotBlank String secret,
        @NotNull Duration tokenValidity,
        @NotNull Duration rememberMeValidity) {
}
