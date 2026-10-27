package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

public interface ClearSyncErrorUseCase {

    // La copia está al día y la cátedra respondió: un error de una vuelta anterior ya no describe el estado actual
    void clearError();
}
