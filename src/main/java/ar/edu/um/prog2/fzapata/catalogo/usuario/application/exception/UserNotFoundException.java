package ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("No existe el usuario con id " + id);
    }
}
