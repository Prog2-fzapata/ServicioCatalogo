package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class UserRegistrationRequest {

    // Patrón de login de JHipster: algo con forma de email o una palabra de letras, números y _ . @ -.
    // Alternativas agrupadas y sin repetir grupos, para evitar el desborde de pila con entradas largas
    public static final String LOGIN_REGEX = "^(?:[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9.-]+|[_.@A-Za-z0-9-]+)$";

    @NotBlank(message = "El login es obligatorio")
    @Size(min = 3, max = 50, message = "El login debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = LOGIN_REGEX, message = "El login contiene caracteres no permitidos")
    private String login;

    @NotNull(message = "La contraseña es obligatoria")
    @Size(min = 4, max = 100, message = "La contraseña debe tener entre 4 y 100 caracteres")
    private String password;

    @Size(max = 50, message = "El nombre admite hasta 50 caracteres")
    private String firstName;

    @Size(max = 50, message = "El apellido admite hasta 50 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    @Size(min = 5, max = 254, message = "El email debe tener entre 5 y 254 caracteres")
    private String email;

    @Size(max = 256, message = "La URL de la imagen admite hasta 256 caracteres")
    private String imageUrl;

    @Size(min = 2, max = 10, message = "El idioma debe tener entre 2 y 10 caracteres")
    private String langKey;
}
