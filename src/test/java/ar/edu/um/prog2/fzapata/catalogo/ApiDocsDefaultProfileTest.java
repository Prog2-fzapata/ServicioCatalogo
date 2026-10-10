package ar.edu.um.prog2.fzapata.catalogo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

import lombok.RequiredArgsConstructor;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ApiDocsDefaultProfileTest {

    private final MockMvc mockMvc;

    // Sin perfil, la documentación de la API está disponible: es parte de lo que se entrega
    @Test
    void openApiDocumentIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
