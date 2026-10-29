package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;

public interface GetProfessionalUseCase {

    // Trae todos los horarios, también los deshabilitados: quien consume decide. Vacío si el id no existe
    Optional<ProfessionalSummary> getProfessional(Long id);
}
