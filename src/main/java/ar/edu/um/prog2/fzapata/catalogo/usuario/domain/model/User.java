package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model;

import java.time.Instant;
import java.util.Set;

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
public class User {
    private Long id;
    private String login;
    // Hash BCrypt; la contraseña en claro nunca forma parte del modelo
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String langKey;
    private boolean activated;
    private Set<String> authorities;
    private Instant createdDate;
    private Instant lastModifiedDate;
}
