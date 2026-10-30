package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
