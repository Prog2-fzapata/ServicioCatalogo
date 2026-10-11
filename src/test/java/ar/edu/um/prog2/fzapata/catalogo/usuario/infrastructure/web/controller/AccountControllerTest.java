package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.service.UserService;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.mapper.AccountDtoMapper;
import lombok.RequiredArgsConstructor;

@WebMvcTest(AccountController.class)
@Import({AccountDtoMapper.class, ProblemDetailsFactory.class, SecurityConfiguration.class, JwtConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AccountControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private Map<String, String> validRequest() {
        Map<String, String> request = new LinkedHashMap<>();
        request.put("login", "maria.perez");
        request.put("password", "clave123");
        request.put("firstName", "Maria");
        request.put("lastName", "Perez");
        request.put("email", "maria@mail.com");
        request.put("langKey", "es");
        return request;
    }

    private String json(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void registerReturnsCreatedWithoutBodyAndPassesTheDataToTheService() throws Exception {
        register(json(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userService).registerUser(user.capture(), eq("clave123"));
        assertThat(user.getValue().getLogin()).isEqualTo("maria.perez");
        assertThat(user.getValue().getEmail()).isEqualTo("maria@mail.com");
        assertThat(user.getValue().getFirstName()).isEqualTo("Maria");
        assertThat(user.getValue().getLastName()).isEqualTo("Perez");
        assertThat(user.getValue().getLangKey()).isEqualTo("es");
    }

    @Test
    void registerWithOnlyTheRequiredFieldsIsAccepted() throws Exception {
        register("{\"login\":\"maria.perez\",\"password\":\"clave123\",\"email\":\"maria@mail.com\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void idActivationAndAuthoritiesSentByTheClientAreIgnored() throws Exception {
        register("{\"login\":\"maria.perez\",\"password\":\"clave123\",\"email\":\"maria@mail.com\","
                + "\"id\":99,\"activated\":false,\"authorities\":[\"ROLE_ADMIN\"]}")
                .andExpect(status().isCreated());

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userService).registerUser(user.capture(), any());
        assertThat(user.getValue().getId()).isNull();
        assertThat(user.getValue().getAuthorities()).isNull();
    }

    @Test
    void missingRequiredFieldsReturnValidationErrorWithTheOffendingFields() throws Exception {
        register("{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/register"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("login", "password", "email")));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @CsvSource({
            "login,ab",
            "login,con espacio",
            "login,bad!login",
            "login,ñandu",
            "password,abc",
            "email,no-es-un-email",
            "langKey,e"
    })
    void invalidValuesAreRejectedNamingTheField(String field, String value) throws Exception {
        Map<String, String> request = validRequest();
        request.put(field, value);

        register(json(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @CsvSource({"maria.perez", "maria_perez-1", "maria@mail.com", "a!b@dominio.com", "123"})
    void loginsWithTheAllowedShapesAreAccepted(String login) throws Exception {
        Map<String, String> request = validRequest();
        request.put("login", login);

        register(json(request)).andExpect(status().isCreated());
    }

    @Test
    void nameLongerThan50CharactersIsRejected() throws Exception {
        Map<String, String> request = validRequest();
        request.put("firstName", "a".repeat(51));

        register(json(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        register("{ esto no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(userService);
    }

    @Test
    void loginAlreadyUsedReturnsItsCode() throws Exception {
        when(userService.registerUser(any(), any())).thenThrow(new LoginAlreadyUsedException());

        register(json(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_ALREADY_USED"));
    }

    @Test
    void emailAlreadyUsedReturnsItsCode() throws Exception {
        when(userService.registerUser(any(), any())).thenThrow(new EmailAlreadyUsedException());

        register(json(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    // Camino de los dos registros simultáneos: la base rechaza al segundo y se traduce igual que el chequeo previo
    @ParameterizedTest
    @CsvSource({"uk_users_login,LOGIN_ALREADY_USED", "uk_users_email,EMAIL_ALREADY_USED"})
    void uniqueConstraintViolationIsTranslatedByConstraintName(String constraint, String code) throws Exception {
        when(userService.registerUser(any(), any())).thenThrow(new DataIntegrityViolationException("error",
                new RuntimeException("duplicate key value violates unique constraint \"" + constraint + "\"")));

        register(json(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void unknownIntegrityViolationReturnsGenericServerError() throws Exception {
        when(userService.registerUser(any(), any())).thenThrow(new DataIntegrityViolationException("detalle interno",
                new RuntimeException("null value in column \"secreto\"")));

        register(json(validRequest()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secreto"))));
    }

    @Test
    void unexpectedErrorReturnsGenericServerErrorWithoutInternalDetails() throws Exception {
        when(userService.registerUser(any(), any())).thenThrow(new IllegalStateException("boom interno"));

        register(json(validRequest()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("boom"))));
    }

    // Errores que arma Spring por su cuenta: también salen en español y con code y path
    @Test
    void wrongHttpMethodReturnsMethodNotAllowedInSpanish() throws Exception {
        mockMvc.perform(get("/api/register").with(jwt()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").value("El método HTTP no está permitido para este recurso"))
                .andExpect(jsonPath("$.path").value("/api/register"));
    }

    @Test
    void unknownRouteReturnsNotFoundInSpanish() throws Exception {
        mockMvc.perform(get("/api/no-existe").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("El recurso solicitado no existe"))
                .andExpect(jsonPath("$.path").value("/api/no-existe"));
    }

    @Test
    void unsupportedContentTypeReturnsUnsupportedMediaTypeInSpanish() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.detail").value("El tipo de contenido enviado no está soportado"));

        verifyNoInteractions(userService);
    }
}
