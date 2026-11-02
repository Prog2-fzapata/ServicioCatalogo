package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SearchProfessionalCategoriesUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchProfessionalCategoriesUseCaseImpl implements SearchProfessionalCategoriesUseCase {

    private final ProfessionalCategoryRepository professionalCategoryRepository;

    @Override
    public PagedResult<ProfessionalCategory> searchCategories(CategoryFilter filter, PageQuery page,
            List<CategorySortOrder> sort) {
        return professionalCategoryRepository.search(filter, page, sort);
    }
}
