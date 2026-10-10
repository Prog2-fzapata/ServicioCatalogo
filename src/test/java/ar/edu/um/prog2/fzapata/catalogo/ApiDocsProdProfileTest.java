package ar.edu.um.prog2.fzapata.catalogo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

import lombok.RequiredArgsConstructor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ApiDocsProdProfileTest {

    private final MockMvc mockMvc;

    @Test
    void openApiDocumentIsNotExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    void swaggerUiIsNotExposed() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
}
