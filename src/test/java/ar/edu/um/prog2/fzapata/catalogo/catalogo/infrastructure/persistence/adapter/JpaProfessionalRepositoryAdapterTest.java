package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases.SearchProfessionalsUseCaseImpl;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

// Contra PostgreSQL real: la búsqueda usa la extensión unaccent (migración V4) y una subconsulta
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaProfessionalRepositoryAdapter.class,
        JpaProfessionalCategoryRepositoryAdapter.class, JpaWeeklyScheduleRepositoryAdapter.class, CatalogMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JpaProfessionalRepositoryAdapterTest {

    private static final List<ProfessionalSortOrder> BY_NAME = ProfessionalSortOrder.byName();

    private final JpaProfessionalRepositoryAdapter adapter;
    private final JpaProfessionalCategoryRepositoryAdapter categoryAdapter;
    private final JpaWeeklyScheduleRepositoryAdapter scheduleAdapter;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository scheduleRepository;
    private final CatalogMapper catalogMapper;
    private final EntityManager entityManager;

    private Professional person(long id, long categoryId, String firstName, String lastName, boolean enabled) {
        Professional professional = professional(id, categoryId);
        professional.setFirstName(firstName);
        professional.setLastName(lastName);
        professional.setEnabled(enabled);
        return professional;
    }

    private WeeklySchedule scheduleWith(long id, long professionalId, boolean enabled) {
        WeeklySchedule schedule = schedule(id, professionalId);
        schedule.setEnabled(enabled);
        return schedule;
    }

    // Categorías 1 (Cardiología) y 2 (Pediatría); ocho profesionales con nombres pensados para las pruebas
    @BeforeEach
    void storeTheCatalog() {
        categoryRepository.saveAll(List.of(catalogMapper.toEntity(category(1)), catalogMapper.toEntity(category(2))));
        professionalRepository.saveAll(List.of(
                person(10, 1, "Ana", "Gómez", true),
                person(11, 1, "Luis", "Muñoz", true),
                person(12, 2, "Ana María", "Pérez", false),
                person(13, 2, "Carlos", "Gomez", true),
                person(14, 2, "Mario", "50% Díaz", true),
                person(15, 1, "Marta", "Dias", true),
                person(16, 1, "Rosa", "Silva", true),
                person(17, 2, "Rosa", "Silva", true)).stream().map(catalogMapper::toEntity).toList());
        scheduleRepository.saveAll(List.of(
                scheduleWith(100, 10, true),
                scheduleWith(101, 12, false),
                scheduleWith(102, 13, true),
                scheduleWith(103, 13, true),
                scheduleWith(104, 14, true),
                scheduleWith(105, 16, false)).stream().map(catalogMapper::toEntity).toList());
        entityManager.flush();
        entityManager.clear();
    }

    private ProfessionalFilter filter(Long categoryId, String name, Boolean enabled, Boolean available) {
        return new ProfessionalFilter(categoryId, name, enabled, available);
    }

    private List<Long> idsOf(ProfessionalFilter filter) {
        return adapter.search(filter, new PageQuery(0, 50), BY_NAME).content().stream()
                .map(Professional::getId).toList();
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void withoutCriteriaReturnsEveryoneSortedByLastNameAndThenFirstName() {
        // Dias, Gomez, Gómez, Muñoz, Pérez, 50% Díaz (los números van primero), Silva, Silva
        PagedResult<Professional> result = adapter.search(filter(null, null, null, null), new PageQuery(0, 50),
                BY_NAME);

        assertThat(result.totalElements()).isEqualTo(8);
        assertThat(result.content()).extracting(Professional::getId)
                .containsExactlyInAnyOrder(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L);
        assertThat(result.content()).extracting(Professional::getLastName)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER.thenComparing(java.util.Comparator.naturalOrder()));
    }

    @Test
    void filtersByCategory() {
        assertThat(idsOf(filter(1L, null, null, null))).containsExactlyInAnyOrder(10L, 11L, 15L, 16L);
        assertThat(idsOf(filter(2L, null, null, null))).containsExactlyInAnyOrder(12L, 13L, 14L, 17L);
        assertThat(idsOf(filter(99L, null, null, null))).isEmpty();
    }

    @Test
    void filtersByEnabled() {
        assertThat(idsOf(filter(null, null, false, null))).containsExactly(12L);
        assertThat(idsOf(filter(null, null, true, null))).hasSize(7).doesNotContain(12L);
    }

    @Test
    void theNameIgnoresUppercaseAndLowercase() {
        assertThat(idsOf(filter(null, "ANA", null, null))).containsExactlyInAnyOrder(10L, 12L);
        assertThat(idsOf(filter(null, "aNa gÓmEz", null, null))).containsExactly(10L);
    }

    @Test
    void theNameIgnoresAccentsInBothDirections() {
        assertThat(idsOf(filter(null, "gomez", null, null))).containsExactlyInAnyOrder(10L, 13L);
        assertThat(idsOf(filter(null, "GÓMEZ", null, null))).containsExactlyInAnyOrder(10L, 13L);
        assertThat(idsOf(filter(null, "munoz", null, null))).containsExactly(11L);
        assertThat(idsOf(filter(null, "pérez", null, null))).containsExactly(12L);
        assertThat(idsOf(filter(null, "perez", null, null))).containsExactly(12L);
    }

    @Test
    void theNameMatchesFirstAndLastNameTogetherAndAPartOfIt() {
        assertThat(idsOf(filter(null, "ana gom", null, null))).containsExactly(10L);
        assertThat(idsOf(filter(null, "maría pé", null, null))).containsExactly(12L);
        assertThat(idsOf(filter(null, "uis mu", null, null))).containsExactly(11L);
    }

    // Un % o un _ escrito por el usuario es texto, no un comodín de LIKE
    @Test
    void wildcardsInTheNameAreLiteralCharacters() {
        assertThat(idsOf(filter(null, "50%", null, null))).containsExactly(14L);
        assertThat(idsOf(filter(null, "%", null, null))).containsExactly(14L);
        assertThat(idsOf(filter(null, "_", null, null))).isEmpty();
        assertThat(idsOf(filter(null, "a_a", null, null))).isEmpty();
        assertThat(idsOf(filter(null, "\\", null, null))).isEmpty();
    }

    @Test
    void availableMeansHavingAtLeastOneEnabledSchedule() {
        // 12 y 16 solo tienen horarios deshabilitados; 11, 15 y 17 no tienen ninguno
        assertThat(idsOf(filter(null, null, null, true))).containsExactlyInAnyOrder(10L, 13L, 14L);
    }

    @Test
    void notAvailableMeansHavingNoEnabledSchedule() {
        assertThat(idsOf(filter(null, null, null, false))).containsExactlyInAnyOrder(11L, 12L, 15L, 16L, 17L);
    }

    @Test
    void combinesEveryCriterionWithAnd() {
        assertThat(idsOf(filter(1L, "ana", true, true))).containsExactly(10L);
        assertThat(idsOf(filter(2L, "gomez", true, true))).containsExactly(13L);
        assertThat(idsOf(filter(2L, null, false, false))).containsExactly(12L);
        assertThat(idsOf(filter(1L, "gomez", null, false))).isEmpty();
    }

    @Test
    void paginatesWithoutRepeatingOrSkippingAndReportsTheTotal() {
        PagedResult<Professional> first = adapter.search(filter(null, null, null, null), new PageQuery(0, 3), BY_NAME);
        PagedResult<Professional> second = adapter.search(filter(null, null, null, null), new PageQuery(1, 3), BY_NAME);
        PagedResult<Professional> third = adapter.search(filter(null, null, null, null), new PageQuery(2, 3), BY_NAME);

        assertThat(first.content()).hasSize(3);
        assertThat(second.content()).hasSize(3);
        assertThat(third.content()).hasSize(2);
        assertThat(first.totalElements()).isEqualTo(8);
        assertThat(third.totalPages()).isEqualTo(3);
        assertThat(java.util.stream.Stream.of(first, second, third).flatMap(page -> page.content().stream())
                .map(Professional::getId)).doesNotHaveDuplicates().hasSize(8);
    }

    @Test
    void aPageBeyondTheEndIsEmptyButStillReportsTheTotal() {
        PagedResult<Professional> result = adapter.search(filter(null, null, null, null), new PageQuery(5, 3),
                BY_NAME);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(8);
    }

    @Test
    void theTotalCountsTheFilteredElementsAndNotTheWholeTable() {
        PagedResult<Professional> result = adapter.search(filter(1L, null, null, null), new PageQuery(0, 2), BY_NAME);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(4);
    }

    @Test
    void sortsInTheRequestedDirectionAndField() {
        List<Long> byIdDescending = adapter.search(filter(null, null, null, null), new PageQuery(0, 50),
                List.of(new ProfessionalSortOrder(Field.ID, Direction.DESC))).content().stream()
                .map(Professional::getId).toList();
        List<String> byFirstName = adapter.search(filter(null, null, null, null), new PageQuery(0, 50),
                List.of(new ProfessionalSortOrder(Field.FIRST_NAME, Direction.ASC))).content().stream()
                .map(Professional::getFirstName).toList();

        assertThat(byIdDescending).containsExactly(17L, 16L, 15L, 14L, 13L, 12L, 11L, 10L);
        assertThat(byFirstName).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    // Dos profesionales con el mismo apellido: el id desempata para que las páginas sean estables
    @Test
    void breaksTiesByIdSoThatPagesAreStable() {
        List<Long> silvas = adapter.search(filter(null, "silva", null, null), new PageQuery(0, 50),
                List.of(new ProfessionalSortOrder(Field.LAST_NAME, Direction.ASC))).content().stream()
                .map(Professional::getId).toList();

        assertThat(silvas).containsExactly(16L, 17L);
    }

    @Test
    void theCategoryAdapterReturnsTheExistingOnesAndIgnoresTheMissing() {
        assertThat(categoryAdapter.findAllById(List.of(1L, 2L, 99L))).extracting(c -> c.getId())
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(categoryAdapter.findAllById(List.of())).isEmpty();
    }

    @Test
    void theScheduleAdapterReturnsOnlyTheEnabledOnesOfTheRequestedProfessionals() {
        assertThat(scheduleAdapter.findEnabledByProfessionalIds(List.of(10L, 12L, 13L, 16L)))
                .extracting(WeeklySchedule::getId).containsExactlyInAnyOrder(100L, 102L, 103L);
        assertThat(scheduleAdapter.findEnabledByProfessionalIds(List.of(11L))).isEmpty();
    }

    // Una página completa cuesta las mismas consultas sin importar cuántos profesionales traiga: no hay una por cada uno
    @Test
    void aWholePageNeedsAFixedNumberOfQueries() {
        SearchProfessionalsUseCaseImpl useCase = new SearchProfessionalsUseCaseImpl(adapter, categoryAdapter,
                scheduleAdapter);
        entityManager.clear();
        statistics().clear();

        PagedResult<ProfessionalSummary> result = useCase.searchProfessionals(filter(null, null, null, null),
                new PageQuery(0, 3), BY_NAME);

        assertThat(result.content()).hasSize(3);
        // La página, el total, las categorías y los horarios
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }
}
