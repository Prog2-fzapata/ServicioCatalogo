package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.List;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;

public interface SearchProfessionalsUseCase {

    PagedResult<ProfessionalSummary> searchProfessionals(ProfessionalFilter filter, PageQuery page,
            List<ProfessionalSortOrder> sort);
}
