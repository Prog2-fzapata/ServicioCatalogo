package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

public interface ApplyCatalogSnapshotUseCase {

    SyncResult applySnapshot(CatalogSnapshot snapshot);
}
