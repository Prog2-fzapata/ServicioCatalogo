package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in;

import java.util.Optional;

public interface AuthenticateUserUseCase {
    // Vacío si las credenciales no son válidas; si lo son, el JWT firmado
    Optional<String> authenticateUser(String username, String rawPassword, boolean rememberMe);
}
