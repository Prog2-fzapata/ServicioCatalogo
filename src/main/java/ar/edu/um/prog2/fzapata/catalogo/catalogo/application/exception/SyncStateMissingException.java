package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception;

// La migración V2 crea la fila única de sync_state (id = 1): si no está, alguien alteró la base.
// No es una base sin sincronizar: esa tiene la fila con la versión local en 0
public class SyncStateMissingException extends RuntimeException {

    public SyncStateMissingException() {
        super("La tabla sync_state está vacía, la base fue alterada");
    }
}
