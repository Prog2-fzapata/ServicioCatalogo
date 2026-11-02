package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
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

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.ProblemDetailsSecurityHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.UserNotFoundException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.service.UserService;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.mapper.AccountDtoMapper;
import lombok.RequiredArgsConstructor;

@WebMvcTest(AccountController.class)
@Import({AccountDtoMapper.class, ProblemDetailsFactory.class, SecurityConfiguration.class, JwtConfiguration.class,
        ProblemDetailsSecurityHandler.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class AccountManagementControllerTest {

    private static final String UPDATE_BODY = "{\"firstName\":\"Mariana\",\"lastName\":\"Gomez\","
            + "\"email\":\"mariana@mail.com\",\"imageUrl\":\"https://img.example/m.png\",\"langKey\":\"en\"}";

    private final MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // Token ya validado: acá se prueba el controller, no la firma
    private JwtRequestPostProcessor userToken(long uid) {
        return jwt().jwt(token -> token.subject("maria.perez").claim("uid", uid).claim("auth", "ROLE_USER"));
    }

    private ResultActions saveAccount(String body, JwtRequestPostProcessor token) throws Exception {
        return mockMvc.perform(post("/api/account").with(token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User storedUser() {
        return User.builder()
                .id(7L)
                .login("maria.perez")
                .passwordHash("hash-secreto")
                .firstName("Maria")
                .lastName("Perez")
                .email("maria@mail.com")
                .imageUrl("https://img.example/maria.png")
                .langKey("es")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    void getAccountReturnsTheDataOfTheUserInTheTokenWithoutThePassword() throws Exception {
        when(userService.getAccount(7L)).thenReturn(storedUser());

        mockMvc.perform(get("/api/account").with(userToken(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.login").value("maria.perez"))
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andExpect(jsonPath("$.lastName").value("Perez"))
                .andExpect(jsonPath("$.email").value("maria@mail.com"))
                .andExpect(jsonPath("$.imageUrl").value("https://img.example/maria.png"))
                .andExpect(jsonPath("$.langKey").value("es"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andExpect(content().string(not(containsString("hash-secreto"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void getAccountWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void getAccountOfAUserThatNoLongerExistsReturns404() throws Exception {
        when(userService.getAccount(7L)).thenThrow(new UserNotFoundException(7L));

        mockMvc.perform(get("/api/account").with(userToken(7L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void saveAccountUpdatesTheUserOfTheTokenAndReturnsOkWithoutBody() throws Exception {
        saveAccount(UPDATE_BODY, userToken(7L))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        ArgumentCaptor<User> changes = ArgumentCaptor.forClass(User.class);
        verify(userService).updateAccount(eq(7L), changes.capture());
        assertThat(changes.getValue().getFirstName()).isEqualTo("Mariana");
        assertThat(changes.getValue().getLastName()).isEqualTo("Gomez");
        assertThat(changes.getValue().getEmail()).isEqualTo("mariana@mail.com");
        assertThat(changes.getValue().getImageUrl()).isEqualTo("https://img.example/m.png");
        assertThat(changes.getValue().getLangKey()).isEqualTo("en");
    }

    @Test
    void saveAccountIgnoresIdLoginPasswordAndAuthoritiesSentInTheBody() throws Exception {
        saveAccount("{\"email\":\"mariana@mail.com\",\"id\":99,\"login\":\"otro\",\"password\":\"nueva\","
                + "\"authorities\":[\"ROLE_ADMIN\"]}", userToken(7L))
                .andExpect(status().isOk());

        ArgumentCaptor<User> changes = ArgumentCaptor.forClass(User.class);
        verify(userService).updateAccount(eq(7L), changes.capture());
        assertThat(changes.getValue().getId()).isNull();
        assertThat(changes.getValue().getLogin()).isNull();
        assertThat(changes.getValue().getPasswordHash()).isNull();
        assertThat(changes.getValue().getAuthorities()).isNull();
    }

    @Test
    void saveAccountWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void saveAccountWithInvalidEmailReturnsValidationError() throws Exception {
        saveAccount("{\"email\":\"no-es-un-email\"}", userToken(7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));

        verifyNoInteractions(userService);
    }

    @Test
    void saveAccountWithoutEmailReturnsValidationError() throws Exception {
        saveAccount("{\"firstName\":\"Mariana\"}", userToken(7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void saveAccountWithEmailOfAnotherUserReturnsItsCode() throws Exception {
        when(userService.updateAccount(eq(7L), any())).thenThrow(new EmailAlreadyUsedException());

        saveAccount(UPDATE_BODY, userToken(7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }
}
