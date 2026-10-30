package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.ProblemDetailsSecurityHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.service.UserService;
import lombok.RequiredArgsConstructor;

@WebMvcTest(AuthenticationController.class)
@Import({ProblemDetailsFactory.class, SecurityConfiguration.class, JwtConfiguration.class,
        ProblemDetailsSecurityHandler.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AuthenticationControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private ResultActions authenticate(String body) throws Exception {
        return mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void validCredentialsReturnTheTokenInTheBodyAndInTheAuthorizationHeader() throws Exception {
        when(userService.authenticateUser("maria.perez", "clave123", false)).thenReturn("el.jwt.firmado");

        authenticate("{\"username\":\"maria.perez\",\"password\":\"clave123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").value("el.jwt.firmado"))
                .andExpect(header().string("Authorization", "Bearer el.jwt.firmado"));
    }

    @Test
    void rememberMeIsPassedToTheService() throws Exception {
        when(userService.authenticateUser("maria.perez", "clave123", true)).thenReturn("jwt-largo");

        authenticate("{\"username\":\"maria.perez\",\"password\":\"clave123\",\"rememberMe\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").value("jwt-largo"));
        verify(userService).authenticateUser("maria.perez", "clave123", true);
    }

    @Test
    void invalidCredentialsReturn401WithoutSayingWhichPartFailed() throws Exception {
        when(userService.authenticateUser(any(), any(), anyBoolean()))
                .thenThrow(new InvalidCredentialsException());

        authenticate("{\"username\":\"maria.perez\",\"password\":\"mala\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.path").value("/api/authenticate"))
                .andExpect(content().string(not(containsString("mala"))));
    }

    @Test
    void missingFieldsReturnValidationErrorNamingThem() throws Exception {
        authenticate("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("username", "password")));

        verifyNoInteractions(userService);
    }

    @Test
    void blankUsernameIsRejected() throws Exception {
        authenticate("{\"username\":\"  \",\"password\":\"clave123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void shortPasswordIsNotAValidationErrorSoTheAnswerIsTheSameAsForAnyWrongPassword() throws Exception {
        when(userService.authenticateUser("maria.perez", "abc", false)).thenThrow(new InvalidCredentialsException());

        authenticate("{\"username\":\"maria.perez\",\"password\":\"abc\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        authenticate("{ no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
