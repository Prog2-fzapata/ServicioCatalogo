package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Nunca incluye la contraseña ni su hash
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String langKey;
    private Set<String> authorities;
}
