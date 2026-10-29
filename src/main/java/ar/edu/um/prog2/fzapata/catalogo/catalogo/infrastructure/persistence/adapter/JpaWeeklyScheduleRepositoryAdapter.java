package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.WeeklyScheduleRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaWeeklyScheduleRepositoryAdapter implements WeeklyScheduleRepository {

    private final JpaWeeklyScheduleRepository jpaWeeklyScheduleRepository;
    private final CatalogMapper catalogMapper;

    @Override
    public List<WeeklySchedule> findEnabledByProfessionalIds(Collection<Long> professionalIds) {
        return jpaWeeklyScheduleRepository.findByProfessionalIdInAndEnabledTrue(professionalIds).stream()
                .map(catalogMapper::toDomainModel)
                .toList();
    }
}
