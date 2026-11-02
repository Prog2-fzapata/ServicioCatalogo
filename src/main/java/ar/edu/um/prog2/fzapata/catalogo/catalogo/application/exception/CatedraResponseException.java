package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception;

// La cátedra respondió algo que no se puede usar: otro 4xx, cuerpo ilegible o datos obligatorios ausentes
public class CatedraResponseException extends CatedraException {

    public CatedraResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
