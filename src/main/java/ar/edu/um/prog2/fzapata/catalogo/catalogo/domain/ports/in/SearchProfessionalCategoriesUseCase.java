package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.List;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;

public interface SearchProfessionalCategoriesUseCase {

    PagedResult<ProfessionalCategory> searchCategories(CategoryFilter filter, PageQuery page,
            List<CategorySortOrder> sort);
}
