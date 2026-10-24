package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;
import lombok.RequiredArgsConstructor;

// Argon2id no limita el largo de la contraseña (BCrypt sí: 72 bytes), así que se usa tal cual, sin hash previo.
// El hash guarda sus propios parámetros: cambiar el costo no invalida los hashes ya guardados
@Component
@RequiredArgsConstructor
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
