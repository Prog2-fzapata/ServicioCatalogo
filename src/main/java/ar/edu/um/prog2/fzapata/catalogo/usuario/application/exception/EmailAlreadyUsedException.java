package ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("El email ya está en uso");
    }
}
