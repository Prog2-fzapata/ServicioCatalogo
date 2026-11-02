package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    // Login o email; 254 es el máximo de un email
    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 254, message = "El usuario admite hasta 254 caracteres")
    private String username;

    // Sin mínimo: una clave corta es una credencial incorrecta (401), no un dato mal formado
    @NotNull(message = "La contraseña es obligatoria")
    @Size(max = 100, message = "La contraseña admite hasta 100 caracteres")
    private String password;

    // Boolean y no boolean: Jackson 3 rechaza un primitivo ausente; si falta, se toma como false
    private Boolean rememberMe;
}
