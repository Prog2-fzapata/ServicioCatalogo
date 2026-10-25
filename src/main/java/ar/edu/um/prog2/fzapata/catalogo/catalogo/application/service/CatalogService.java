package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;

    // Vacío no es un error: el evento ya estaba procesado o la copia local ya estaba al día
    public Optional<SyncResult> synchronizeForEvent(CatalogUpdatedEvent event) {
        return synchronizeCatalogUseCase.synchronizeForEvent(event);
    }
}
