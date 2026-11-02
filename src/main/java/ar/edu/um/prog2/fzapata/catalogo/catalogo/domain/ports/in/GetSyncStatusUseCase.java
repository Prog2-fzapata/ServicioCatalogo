package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;

public interface GetSyncStatusUseCase {

    // Vacío solo si falta la fila única de sync_state: una base sin sincronizar tiene la fila con versión 0
    Optional<SyncState> getSyncStatus();
}
