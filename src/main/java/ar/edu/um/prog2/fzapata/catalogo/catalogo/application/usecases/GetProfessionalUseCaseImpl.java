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

    private List<WeeklySchedule> schedules(Professional professional) {
        return weeklyScheduleRepository.findByProfessionalId(professional.getId()).stream()
                .sorted(WeeklySchedule.BY_DAY_AND_START_TIME).toList();
    }

    // La clave foránea garantiza que la categoría existe; deshabilitada o no, se informa igual. Si faltara, no se
    // sabe que esté habilitada: se informa como deshabilitada para no ofrecer horarios por error
    private ProfessionalSummary summary(Professional professional) {
        Optional<ProfessionalCategory> category = professionalCategoryRepository
                .findById(professional.getCategoryId());
        return new ProfessionalSummary(professional, category.map(ProfessionalCategory::getName).orElse(null),
                category.map(ProfessionalCategory::isEnabled).orElse(false), schedules(professional));
    }

    @Override
    public Optional<ProfessionalSummary> getProfessional(Long id) {
        return professionalRepository.findById(id).map(this::summary);
    }
}
