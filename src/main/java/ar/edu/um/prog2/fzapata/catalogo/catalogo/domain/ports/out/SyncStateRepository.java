package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;

public interface SyncStateRepository {

    // Bloquea la fila hasta el fin de la transacción: serializa las sincronizaciones concurrentes
    Optional<SyncState> findForUpdate();

    void save(SyncState state);
}
