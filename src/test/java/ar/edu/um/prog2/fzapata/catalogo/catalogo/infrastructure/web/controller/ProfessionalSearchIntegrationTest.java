package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;

// Todo real: un usuario registrado, el filtro JWT, el controller, la búsqueda con unaccent y PostgreSQL
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@Transactional
class ProfessionalSearchIntegrationTest {

    private final MockMvc mockMvc;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository scheduleRepository;
    private final CatalogMapper catalogMapper;

    private String token;

    private Professional person(long id, long categoryId, String firstName, String lastName, boolean enabled) {
        Professional professional = professional(id, categoryId);
        professional.setFirstName(firstName);
        professional.setLastName(lastName);
        professional.setEnabled(enabled);
        return professional;
    }

    private WeeklySchedule scheduleOf(long id, long professionalId, DayOfWeek day, int hour, boolean enabled) {
        WeeklySchedule schedule = schedule(id, professionalId);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(hour, 0));
        schedule.setEndTime(LocalTime.of(hour + 3, 0));
        schedule.setEnabled(enabled);
        return schedule;
    }

    // Categorías 1 y 2; cinco profesionales y los horarios de dos de ellos (uno deshabilitado)
    @BeforeEach
    void registerLoginAndStoreTheCatalog() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"login":"maria.busca","password":"clave123","email":"maria.busca@mail.com",
                 "firstName":"Maria","lastName":"Perez"}""")).andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"maria.busca\",\"password\":\"clave123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(response, "$.id_token");

        categoryRepository.saveAll(List.of(catalogMapper.toEntity(category(1)), catalogMapper.toEntity(category(2))));
        professionalRepository.saveAll(List.of(
                person(10, 1, "Ana", "Gómez", true),
                person(11, 1, "Luis", "Muñoz", true),
                person(12, 2, "Ana María", "Pérez", false),
                person(13, 2, "Carlos", "Gomez", true),
                person(14, 1, "Rosa", "Silva", true)).stream().map(catalogMapper::toEntity).toList());
        scheduleRepository.saveAll(List.of(
                scheduleOf(100, 10, DayOfWeek.WEDNESDAY, 14, true),
                scheduleOf(101, 10, DayOfWeek.MONDAY, 9, true),
                scheduleOf(102, 10, DayOfWeek.TUESDAY, 9, false),
                scheduleOf(103, 13, DayOfWeek.FRIDAY, 8, true)).stream().map(catalogMapper::toEntity).toList());
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get(URI.create("/api/professionals" + query)).header("Authorization", "Bearer " + token));
    }

    private List<Integer> idsOf(String query) throws Exception {
        String body = search(query).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/professionals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void anAuthenticatedUserSeesEveryProfessionalWithTheTotalInTheHeader() throws Exception {
        search("").andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void theNameIgnoresAccentsAndUppercaseThroughTheWholeStack() throws Exception {
        assertThat(idsOf("?name=gomez")).containsExactlyInAnyOrder(10, 13);
        assertThat(idsOf("?name=G%C3%93MEZ")).containsExactlyInAnyOrder(10, 13);
        assertThat(idsOf("?name=munoz")).containsExactly(11);
        assertThat(idsOf("?name=ana%20gom")).containsExactly(10);
    }

    // Un % en la URL llega como el carácter %, no como un comodín que devuelva todo
    @Test
    void aPercentSignInTheNameIsATextAndNotAWildcard() throws Exception {
        assertThat(idsOf("?name=%25")).isEmpty();
    }

    @Test
    void filtersByCategoryEnabledAndAvailability() throws Exception {
        assertThat(idsOf("?categoryId=2")).containsExactlyInAnyOrder(12, 13);
        assertThat(idsOf("?enabled=false")).containsExactly(12);
        assertThat(idsOf("?available=true")).containsExactlyInAnyOrder(10, 13);
        assertThat(idsOf("?available=false")).containsExactlyInAnyOrder(11, 12, 14);
        assertThat(idsOf("?categoryId=1&enabled=true&available=false")).containsExactlyInAnyOrder(11, 14);
    }

    @Test
    void theListShowsTheCategoryNameAndOnlyTheEnabledSchedulesInOrder() throws Exception {
        String body = search("?name=ana%20gomez").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();

        assertThat(JsonPath.<String>read(body, "$[0].categoryName")).isEqualTo("Categoria 1");
        assertThat(JsonPath.<List<String>>read(body, "$[0].weeklySchedules[*].dayOfWeek"))
                .containsExactly("MONDAY", "WEDNESDAY");
        assertThat(JsonPath.<List<String>>read(body, "$[0].weeklySchedules[*].startTime"))
                .containsExactly("09:00:00", "14:00:00");
    }

    @Test
    void sortsByTheRequestedFieldAndDirection() throws Exception {
        assertThat(idsOf("?sort=firstName,desc")).containsExactly(14, 11, 13, 12, 10);
        assertThat(idsOf("?sort=id,desc")).containsExactly(14, 13, 12, 11, 10);
    }

    @Test
    void paginatesAndTellsHowToReachTheOtherPages() throws Exception {
        search("?size=2&page=1&sort=id,asc").andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(header().string("Link", containsString("page=2&size=2>; rel=\"next\"")))
                .andExpect(header().string("Link", containsString("page=0&size=2>; rel=\"prev\"")))
                .andExpect(header().string("Link", containsString("sort=id,asc")));
        search("?size=2&page=2&sort=id,asc").andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(14));
    }

    @Test
    void theLinksKeepTheEncodedFiltersOfTheRequest() throws Exception {
        search("?name=ana%20g%C3%B3mez&size=1").andExpect(status().isOk())
                .andExpect(header().string("Link", containsString("name=ana%20g%C3%B3mez")));
    }

    // CATALOGO_PAGE_MAX_SIZE vale 100 por defecto: un size mayor se reduce y el enlace lo refleja
    @Test
    void aSizeAboveTheMaximumIsReducedToTheConfiguredMaximum() throws Exception {
        search("?size=5000").andExpect(status().isOk())
                .andExpect(header().string("Link", containsString("size=100")));
    }

    @Test
    void aSortFieldThatIsNotAllowedIsABadRequest() throws Exception {
        search("?sort=enabled").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void doesNotSupportWriting() throws Exception {
        mockMvc.perform(post("/api/professionals").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }
}
