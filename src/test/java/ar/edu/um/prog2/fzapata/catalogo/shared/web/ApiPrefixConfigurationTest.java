package ar.edu.um.prog2.fzapata.catalogo.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

// Sin la seguridad: lo que se prueba es solo cómo se arma la ruta de un controller de la aplicación
@WebMvcTest(ApiPrefixConfigurationTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiPrefixConfiguration.class, ApiPrefixConfigurationTest.ProbeController.class,
        ProblemDetailsFactory.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ApiPrefixConfigurationTest {

    @RestController
    static class ProbeController {

        @GetMapping("/probe")
        String probe() {
            return "ok";
        }
    }

    private final MockMvc mockMvc;

    @Test
    void aControllerOfTheApplicationIsServedUnderTheApiPrefix() throws Exception {
        mockMvc.perform(get("/api/probe")).andExpect(status().isOk()).andExpect(content().string("ok"));
    }

    @Test
    void itIsNotServedWithoutThePrefix() throws Exception {
        mockMvc.perform(get("/probe")).andExpect(status().isNotFound());
    }
}
