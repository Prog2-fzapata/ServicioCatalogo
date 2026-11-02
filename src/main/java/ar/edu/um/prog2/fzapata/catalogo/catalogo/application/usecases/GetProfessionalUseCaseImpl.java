package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetProfessionalUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.WeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetProfessionalUseCaseImpl implements GetProfessionalUseCase {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalCategoryRepository professionalCategoryRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;

    // La clave foránea garantiza que la categoría existe; deshabilitada o no, se informa igual
    private String categoryName(Professional professional) {
        return professionalCategoryRepository.findById(professional.getCategoryId())
                .map(ProfessionalCategory::getName).orElse(null);
    }

    private List<WeeklySchedule> schedules(Professional professional) {
        return weeklyScheduleRepository.findByProfessionalId(professional.getId()).stream()
                .sorted(WeeklySchedule.BY_DAY_AND_START_TIME).toList();
    }

    @Override
    public Optional<ProfessionalSummary> getProfessional(Long id) {
        return professionalRepository.findById(id).map(professional -> new ProfessionalSummary(professional,
                categoryName(professional), schedules(professional)));
    }
}
