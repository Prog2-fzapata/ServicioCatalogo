package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;

// Todo real: un usuario registrado, el filtro JWT, el controller, el caso de uso y PostgreSQL
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@Transactional
class ProfessionalDetailIntegrationTest {

    private final MockMvc mockMvc;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository scheduleRepository;
    private final CatalogMapper catalogMapper;

    private String token;

    private WeeklySchedule scheduleOf(long id, long professionalId, DayOfWeek day, int hour, boolean enabled) {
        WeeklySchedule schedule = schedule(id, professionalId);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(hour, 0));
        schedule.setEndTime(LocalTime.of(hour + 3, 0));
        schedule.setEnabled(enabled);
        return schedule;
    }

    // La categoría 1 habilitada y la 2 deshabilitada. El 10 tiene tres horarios (uno deshabilitado), el 11 está
    // deshabilitado y no tiene horarios, el 12 es de la categoría deshabilitada
    @BeforeEach
    void registerLoginAndStoreTheCatalog() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"login":"maria.detalle","password":"clave123","email":"maria.detalle@mail.com",
                 "firstName":"Maria","lastName":"Perez"}""")).andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"maria.detalle\",\"password\":\"clave123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(response, "$.id_token");

        ProfessionalCategory disabledCategory = category(2);
        disabledCategory.setEnabled(false);
        categoryRepository.saveAll(List.of(catalogMapper.toEntity(category(1)),
                catalogMapper.toEntity(disabledCategory)));
        Professional disabledProfessional = professional(11, 1);
        disabledProfessional.setEnabled(false);
        professionalRepository.saveAll(List.of(professional(10, 1), disabledProfessional, professional(12, 2))
                .stream().map(catalogMapper::toEntity).toList());
        scheduleRepository.saveAll(List.of(
                scheduleOf(100, 10, DayOfWeek.WEDNESDAY, 14, true),
                scheduleOf(101, 10, DayOfWeek.MONDAY, 9, true),
                scheduleOf(102, 10, DayOfWeek.TUESDAY, 9, false)).stream().map(catalogMapper::toEntity).toList());
    }

    private ResultActions detail(String id) throws Exception {
        return mockMvc.perform(get("/api/professionals/" + id).header("Authorization", "Bearer " + token));
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/professionals/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsTheProfessionalWithItsCategoryNameAndEverySchedule() throws Exception {
        String body = detail("10").andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Categoria 1"))
                .andExpect(jsonPath("$.firstName").value("Nombre10"))
                .andExpect(jsonPath("$.lastName").value("Apellido10"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<Integer>>read(body, "$.weeklySchedules[*].id")).containsExactly(101, 102, 100);
        assertThat(JsonPath.<List<String>>read(body, "$.weeklySchedules[*].dayOfWeek"))
                .containsExactly("MONDAY", "TUESDAY", "WEDNESDAY");
        assertThat(JsonPath.<List<Boolean>>read(body, "$.weeklySchedules[*].enabled"))
                .containsExactly(true, false, true);
        assertThat(JsonPath.<List<String>>read(body, "$.weeklySchedules[*].startTime"))
                .containsExactly("09:00:00", "09:00:00", "14:00:00");
        assertThat(JsonPath.<List<Integer>>read(body, "$.weeklySchedules[*].slotDurationMinutes"))
                .containsExactly(30, 30, 30);
    }

    @Test
    void aDisabledProfessionalIsReturnedWithEnabledFalseAndNoSchedules() throws Exception {
        detail("11").andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.weeklySchedules.length()").value(0));
    }

    @Test
    void aProfessionalOfADisabledCategoryIsReturnedAnyway() throws Exception {
        detail("12").andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.categoryName").value("Categoria 2"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void anUnknownProfessionalIsANotFound() throws Exception {
        detail("99").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No existe el profesional con id 99"));
    }

    @Test
    void anIdThatIsNotANumberIsABadRequest() throws Exception {
        detail("abc").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
