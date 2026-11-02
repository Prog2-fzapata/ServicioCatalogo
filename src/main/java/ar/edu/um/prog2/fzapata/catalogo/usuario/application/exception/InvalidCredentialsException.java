package ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception;

public class InvalidCredentialsException extends RuntimeException {
    // Un solo mensaje para usuario inexistente, contraseña errónea o cuenta inactiva: no revela cuál fue
    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
