package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
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
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import lombok.RequiredArgsConstructor;

// Todo real: un usuario registrado, el filtro JWT, el controller, el caso de uso y PostgreSQL
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@Transactional
class CategorySearchIntegrationTest {

    private final MockMvc mockMvc;
    private final JpaProfessionalCategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;

    private String token;

    private ProfessionalCategory named(long id, String name, boolean enabled) {
        ProfessionalCategory category = category(id);
        category.setName(name);
        category.setEnabled(enabled);
        return category;
    }

    // Por nombre: Cardiología (2), Dermatología (4), Neurología (3, deshabilitada), Pediatría (1), Traumatología (5)
    @BeforeEach
    void registerLoginAndStoreTheCategories() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"login":"maria.categorias","password":"clave123","email":"maria.categorias@mail.com",
                 "firstName":"Maria","lastName":"Perez"}""")).andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"maria.categorias\",\"password\":\"clave123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(response, "$.id_token");

        categoryRepository.saveAll(List.of(
                named(1, "Pediatría", true),
                named(2, "Cardiología", true),
                named(3, "Neurología", false),
                named(4, "Dermatología", true),
                named(5, "Traumatología", true)).stream().map(catalogMapper::toEntity).toList());
    }

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get(URI.create("/api/professional-categories" + query))
                .header("Authorization", "Bearer " + token));
    }

    private List<Integer> idsOf(String query) throws Exception {
        String body = search(query).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/professional-categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listsEveryCategorySortedByNameWithTheTotalInTheHeader() throws Exception {
        search("").andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Cardiología"))
                .andExpect(jsonPath("$[0].description").value("Descripcion 2"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[2].enabled").value(false));
        assertThat(idsOf("")).containsExactly(2, 4, 3, 1, 5);
    }

    @Test
    void filtersByEnabled() throws Exception {
        assertThat(idsOf("?enabled=true")).containsExactly(2, 4, 1, 5);
        assertThat(idsOf("?enabled=false")).containsExactly(3);
    }

    @Test
    void sortsByTheRequestedFieldAndDirection() throws Exception {
        assertThat(idsOf("?sort=name,desc")).containsExactly(5, 1, 3, 4, 2);
        assertThat(idsOf("?sort=id,desc")).containsExactly(5, 4, 3, 2, 1);
    }

    @Test
    void paginatesAndTellsHowToReachTheOtherPages() throws Exception {
        search("?size=2&page=1").andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(header().string("Link", containsString("page=2&size=2>; rel=\"next\"")))
                .andExpect(header().string("Link", containsString("page=0&size=2>; rel=\"prev\"")));
        search("?size=2&page=2").andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void theLinksKeepTheFilterOfTheRequest() throws Exception {
        search("?enabled=true&size=1").andExpect(status().isOk())
                .andExpect(header().string("Link", containsString("enabled=true")));
    }

    // CATALOGO_PAGE_MAX_SIZE vale 100 por defecto: un size mayor se reduce y el enlace lo refleja
    @Test
    void aSizeAboveTheMaximumIsReducedToTheConfiguredMaximum() throws Exception {
        search("?size=5000").andExpect(status().isOk())
                .andExpect(header().string("Link", containsString("size=100")));
    }

    @Test
    void aSortFieldThatIsNotAllowedIsABadRequest() throws Exception {
        search("?sort=description").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].objectName").value("categorySearch"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void doesNotSupportWriting() throws Exception {
        mockMvc.perform(post("/api/professional-categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }
}
