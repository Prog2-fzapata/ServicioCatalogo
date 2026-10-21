package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.CREATED;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.UPDATED;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaCatalogRepositoryAdapter.class, CatalogMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JpaCatalogRepositoryAdapterTest {

    private final JpaCatalogRepositoryAdapter adapter;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository scheduleRepository;
    private final EntityManager entityManager;

    private CatalogSnapshot snapshot(List<ProfessionalCategory> categories, List<Professional> professionals,
            List<WeeklySchedule> schedules) {
        return CatalogFixtures.snapshot(1, categories, professionals, schedules);
    }

    // Escribe en la base y vacía la memoria de Hibernate: lo que se lee después viene de la base
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void store(CatalogSnapshot snapshot) {
        adapter.replaceAll(snapshot);
        flushAndClear();
    }

    private <E> List<Long> ids(List<E> entities, Function<E, Long> idOf) {
        return entities.stream().map(idOf).sorted().toList();
    }

    private List<Long> categoryIds() {
        return ids(categoryRepository.findAll(), ProfessionalCategoryEntity::getId);
    }

    private List<Long> professionalIds() {
        return ids(professionalRepository.findAll(), ProfessionalEntity::getId);
    }

    private List<Long> scheduleIds() {
        return ids(scheduleRepository.findAll(), WeeklyScheduleEntity::getId);
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void storesEveryFieldOfTheThreeCollectionsInEmptyTables() {
        store(snapshot(List.of(category(1)), List.of(professional(10, 1)), List.of(schedule(100, 10))));

        ProfessionalCategoryEntity category = categoryRepository.findById(1L).orElseThrow();
        assertThat(category.getName()).isEqualTo("Categoria 1");
        assertThat(category.getDescription()).isEqualTo("Descripcion 1");
        assertThat(category.isEnabled()).isTrue();
        assertThat(category.getCreatedAt()).isEqualTo(CREATED);
        assertThat(category.getUpdatedAt()).isEqualTo(UPDATED);
        ProfessionalEntity professional = professionalRepository.findById(10L).orElseThrow();
        assertThat(professional.getCategoryId()).isEqualTo(1L);
        assertThat(professional.getFirstName()).isEqualTo("Nombre10");
        assertThat(professional.getLastName()).isEqualTo("Apellido10");
        WeeklyScheduleEntity schedule = scheduleRepository.findById(100L).orElseThrow();
        assertThat(schedule.getProfessionalId()).isEqualTo(10L);
        assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 30));
        assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(schedule.isEnabled()).isTrue();
    }

    @Test
    void writesOnlyTheRowsThatChanged() {
        store(snapshot(List.of(category(1)), List.of(professional(10, 1), professional(11, 1)),
                List.of(schedule(100, 10))));
        statistics().clear();

        Professional changed = professional(11, 1);
        changed.setEnabled(false);
        store(snapshot(List.of(category(1)), List.of(professional(10, 1), changed), List.of(schedule(100, 10))));

        assertThat(statistics().getEntityUpdateCount()).isEqualTo(1);
        assertThat(statistics().getEntityInsertCount()).isZero();
        assertThat(statistics().getEntityDeleteCount()).isZero();
        assertThat(professionalRepository.findById(11L).orElseThrow().isEnabled()).isFalse();
        assertThat(professionalRepository.findById(10L).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void createsTheNewRowsAndDeletesTheOnesThatNoLongerArrive() {
        store(snapshot(List.of(category(1)), List.of(professional(10, 1), professional(11, 1)),
                List.of(schedule(100, 10), schedule(101, 11))));

        store(snapshot(List.of(category(1), category(2)), List.of(professional(10, 1), professional(12, 2)),
                List.of(schedule(100, 10), schedule(102, 12))));

        assertThat(categoryIds()).containsExactly(1L, 2L);
        assertThat(professionalIds()).containsExactly(10L, 12L);
        assertThat(scheduleIds()).containsExactly(100L, 102L);
    }

    @Test
    void deletesChildrenBeforeParentsWhenAWholeBranchDisappears() {
        store(snapshot(List.of(category(1), category(2)), List.of(professional(10, 1), professional(20, 2)),
                List.of(schedule(100, 10), schedule(200, 20))));

        store(snapshot(List.of(category(1)), List.of(professional(10, 1)), List.of(schedule(100, 10))));

        assertThat(categoryIds()).containsExactly(1L);
        assertThat(professionalIds()).containsExactly(10L);
        assertThat(scheduleIds()).containsExactly(100L);
    }

    @Test
    void movesAScheduleToAnotherProfessionalWhileTheOldOneIsDeleted() {
        store(snapshot(List.of(category(1)), List.of(professional(10, 1)), List.of(schedule(100, 10))));

        store(snapshot(List.of(category(1)), List.of(professional(11, 1)), List.of(schedule(100, 11))));

        assertThat(professionalIds()).containsExactly(11L);
        assertThat(scheduleRepository.findById(100L).orElseThrow().getProfessionalId()).isEqualTo(11L);
    }

    @Test
    void anEmptySnapshotEmptiesTheTables() {
        store(snapshot(List.of(category(1)), List.of(professional(10, 1)), List.of(schedule(100, 10))));

        store(snapshot(List.of(), List.of(), List.of()));

        assertThat(categoryIds()).isEmpty();
        assertThat(professionalIds()).isEmpty();
        assertThat(scheduleIds()).isEmpty();
    }

    @Test
    void failsWhenAProfessionalPointsToACategoryThatDoesNotArrive() {
        CatalogSnapshot invalid = snapshot(List.of(category(1)), List.of(professional(10, 99)), List.of());

        assertThatThrownBy(() -> store(invalid)).isInstanceOf(PersistenceException.class);
    }
}
