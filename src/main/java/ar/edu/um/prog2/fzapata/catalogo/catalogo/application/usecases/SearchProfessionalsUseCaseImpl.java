package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.WeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchProfessionalsUseCaseImpl implements SearchProfessionalsUseCase {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalCategoryRepository professionalCategoryRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;

    private Map<Long, String> categoryNames(List<Professional> professionals) {
        Set<Long> categoryIds = professionals.stream().map(Professional::getCategoryId).collect(Collectors.toSet());
        return professionalCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(ProfessionalCategory::getId, ProfessionalCategory::getName));
    }

    // Lunes primero y, dentro de un día, por hora de inicio: así el resumen se lee de corrido
    private Map<Long, List<WeeklySchedule>> schedulesByProfessional(List<Professional> professionals) {
        Set<Long> professionalIds = professionals.stream().map(Professional::getId).collect(Collectors.toSet());
        return weeklyScheduleRepository.findEnabledByProfessionalIds(professionalIds).stream()
                .sorted(Comparator.comparing(WeeklySchedule::getDayOfWeek)
                        .thenComparing(WeeklySchedule::getStartTime))
                .collect(Collectors.groupingBy(WeeklySchedule::getProfessionalId));
    }

    // Tres consultas por página sin importar cuántos profesionales traiga: la página, las categorías y los horarios
    @Override
    public PagedResult<ProfessionalSummary> searchProfessionals(ProfessionalFilter filter, PageQuery page,
            List<ProfessionalSortOrder> sort) {
        PagedResult<Professional> found = professionalRepository.search(filter, page, sort);
        if (found.content().isEmpty()) {
            return new PagedResult<>(List.of(), found.totalElements(), found.pageQuery());
        }
        Map<Long, String> categoryNames = categoryNames(found.content());
        Map<Long, List<WeeklySchedule>> schedules = schedulesByProfessional(found.content());
        return found.map(professional -> new ProfessionalSummary(professional,
                categoryNames.get(professional.getCategoryId()),
                schedules.getOrDefault(professional.getId(), List.of())));
    }
}
