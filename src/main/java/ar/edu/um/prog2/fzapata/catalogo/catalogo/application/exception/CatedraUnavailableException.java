package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception;

// Sin conexión, timeout o 5xx: la cátedra no está disponible ahora; vale reintentar más tarde
public class CatedraUnavailableException extends CatedraException {

    public CatedraUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
