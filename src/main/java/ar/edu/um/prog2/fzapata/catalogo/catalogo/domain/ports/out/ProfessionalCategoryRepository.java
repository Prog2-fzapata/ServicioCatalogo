package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;

public interface ProfessionalCategoryRepository {

    // Los ids que no existen simplemente no aparecen en el resultado
    List<ProfessionalCategory> findAllById(Collection<Long> ids);

    Optional<ProfessionalCategory> findById(Long id);

    // Siempre sobre la copia local. Desempata por id para que las páginas sean estables
    PagedResult<ProfessionalCategory> search(CategoryFilter filter, PageQuery page, List<CategorySortOrder> sort);
}
