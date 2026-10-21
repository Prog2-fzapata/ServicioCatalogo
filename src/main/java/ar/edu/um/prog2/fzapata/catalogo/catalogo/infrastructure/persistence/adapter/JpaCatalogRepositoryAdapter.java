package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatalogRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaCatalogRepositoryAdapter implements CatalogRepository {

    private final JpaProfessionalCategoryRepository jpaProfessionalCategoryRepository;
    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final JpaWeeklyScheduleRepository jpaWeeklyScheduleRepository;
    private final CatalogMapper catalogMapper;

    @Override
    public void replaceAll(CatalogSnapshot snapshot) {
        // Se leen las tres tablas primero: las filas quedan en la memoria de Hibernate, así el save de cada
        // elemento sabe cuál ya existe sin consultar de a una y solo escribe UPDATE si algo cambió
        List<ProfessionalCategoryEntity> currentCategories = jpaProfessionalCategoryRepository.findAll();
        List<ProfessionalEntity> currentProfessionals = jpaProfessionalRepository.findAll();
        List<WeeklyScheduleEntity> currentSchedules = jpaWeeklyScheduleRepository.findAll();

        // Se escribe de padres a hijos y se borra de hijos a padres por las claves foráneas
        jpaProfessionalCategoryRepository.saveAll(
                snapshot.getCategories().stream().map(catalogMapper::toEntity).toList());
        jpaProfessionalRepository.saveAll(
                snapshot.getProfessionals().stream().map(catalogMapper::toEntity).toList());
        jpaWeeklyScheduleRepository.saveAll(
                snapshot.getWeeklySchedules().stream().map(catalogMapper::toEntity).toList());

        jpaWeeklyScheduleRepository.deleteAll(missing(currentSchedules, WeeklyScheduleEntity::getId,
                idsOf(snapshot.getWeeklySchedules(), WeeklySchedule::getId)));
        jpaProfessionalRepository.deleteAll(missing(currentProfessionals, ProfessionalEntity::getId,
                idsOf(snapshot.getProfessionals(), Professional::getId)));
        jpaProfessionalCategoryRepository.deleteAll(missing(currentCategories, ProfessionalCategoryEntity::getId,
                idsOf(snapshot.getCategories(), ProfessionalCategory::getId)));
    }

    private <T> Set<Long> idsOf(List<T> items, Function<T, Long> idOf) {
        return items.stream().map(idOf).collect(Collectors.toSet());
    }

    // Filas que están en la base y ya no llegan en el snapshot
    private <E> List<E> missing(List<E> current, Function<E, Long> idOf, Set<Long> snapshotIds) {
        return current.stream().filter(entity -> !snapshotIds.contains(idOf.apply(entity))).toList();
    }
}
