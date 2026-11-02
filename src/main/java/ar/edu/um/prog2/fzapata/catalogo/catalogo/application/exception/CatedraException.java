package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception;

// Base de los fallos al hablar con la cátedra; quien llama decide cómo reaccionar a cada subtipo
public abstract class CatedraException extends RuntimeException {

    protected CatedraException(String message, Throwable cause) {
        super(message, cause);
    }
}
