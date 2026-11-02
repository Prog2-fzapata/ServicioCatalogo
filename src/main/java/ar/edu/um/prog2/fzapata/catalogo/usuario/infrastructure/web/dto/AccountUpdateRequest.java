package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class AccountUpdateRequest {

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
