package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaProfessionalCategoryRepositoryAdapter implements ProfessionalCategoryRepository {

    private final JpaProfessionalCategoryRepository jpaProfessionalCategoryRepository;
    private final CatalogMapper catalogMapper;

    @Override
    public List<ProfessionalCategory> findAllById(Collection<Long> ids) {
        return jpaProfessionalCategoryRepository.findAllById(ids).stream()
                .map(catalogMapper::toDomainModel)
                .toList();
    }
}
