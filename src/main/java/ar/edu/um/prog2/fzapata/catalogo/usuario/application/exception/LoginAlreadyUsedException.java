package ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception;

public class LoginAlreadyUsedException extends RuntimeException {
    public LoginAlreadyUsedException() {
        super("El login ya está en uso");
    }
}
