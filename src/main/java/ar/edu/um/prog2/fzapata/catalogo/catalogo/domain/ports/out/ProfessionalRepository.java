package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import java.util.List;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;

public interface ProfessionalRepository {

    // Siempre sobre la copia local. Desempata por id para que las páginas sean estables
    PagedResult<Professional> search(ProfessionalFilter filter, PageQuery page, List<ProfessionalSortOrder> sort);
}
