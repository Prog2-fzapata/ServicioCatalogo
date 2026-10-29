package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import java.util.Collection;
import java.util.List;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;

public interface ProfessionalCategoryRepository {

    // Los ids que no existen simplemente no aparecen en el resultado
    List<ProfessionalCategory> findAllById(Collection<Long> ids);
}
