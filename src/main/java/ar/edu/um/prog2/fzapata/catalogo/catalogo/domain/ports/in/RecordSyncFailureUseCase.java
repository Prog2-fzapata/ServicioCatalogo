package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;

public interface RecordSyncFailureUseCase {

    void recordFailure(SyncType type, Throwable cause);
}
