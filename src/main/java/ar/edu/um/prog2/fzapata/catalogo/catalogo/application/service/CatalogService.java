package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.ProfessionalNotFoundException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetProfessionalUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetSyncStatusUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SearchProfessionalCategoriesUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;
    private final GetSyncStatusUseCase getSyncStatusUseCase;
    private final SearchProfessionalsUseCase searchProfessionalsUseCase;
    private final GetProfessionalUseCase getProfessionalUseCase;
    private final SearchProfessionalCategoriesUseCase searchProfessionalCategoriesUseCase;

    // Vacío no es un error: la copia local ya está al día
    public Optional<SyncResult> synchronize() {
        return synchronizeCatalogUseCase.synchronize();
    }

    // Vacío no es un error: el evento ya estaba procesado o la copia local ya estaba al día
    public Optional<SyncResult> synchronizeForEvent(CatalogUpdatedEvent event) {
        return synchronizeCatalogUseCase.synchronizeForEvent(event);
    }

    public SyncState getSyncStatus() {
        return getSyncStatusUseCase.getSyncStatus().orElseThrow(SyncStateMissingException::new);
    }

    public PagedResult<ProfessionalSummary> searchProfessionals(ProfessionalFilter filter, PageQuery page,
            List<ProfessionalSortOrder> sort) {
        return searchProfessionalsUseCase.searchProfessionals(filter, page, sort);
    }

    public ProfessionalSummary getProfessional(Long id) {
        return getProfessionalUseCase.getProfessional(id).orElseThrow(() -> new ProfessionalNotFoundException(id));
    }

    public PagedResult<ProfessionalCategory> searchCategories(CategoryFilter filter, PageQuery page,
            List<CategorySortOrder> sort) {
        return searchProfessionalCategoriesUseCase.searchCategories(filter, page, sort);
    }
}
