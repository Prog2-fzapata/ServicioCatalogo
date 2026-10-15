package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception;

// 401 o 403: el JWT técnico es inválido, venció o no tiene permiso; reintentar no sirve, hay que cambiar la configuración
public class
CatedraAuthenticationException extends CatedraException {

    public CatedraAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
