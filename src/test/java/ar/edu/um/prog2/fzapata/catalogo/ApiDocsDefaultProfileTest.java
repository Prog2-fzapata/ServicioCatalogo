package ar.edu.um.prog2.fzapata.catalogo;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    // Con este esquema Swagger UI muestra el botón «Authorize»; el registro y el login no piden token
    @Test
    void openApiDeclaresBearerJwtAndLeavesOnlyThePublicEndpointsOpen() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0]['bearer-jwt']").exists())
                .andExpect(jsonPath("$.paths['/api/register'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/api/authenticate'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/api/account'].get.security").doesNotExist());
    }
}
