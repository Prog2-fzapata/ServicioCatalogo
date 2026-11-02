package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.SyncStatusDtoMapper;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.ProblemDetailsSecurityHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.GlobalExceptionHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import lombok.RequiredArgsConstructor;

@WebMvcTest(SyncStatusController.class)
@Import({SyncStatusDtoMapper.class, ProblemDetailsFactory.class, GlobalExceptionHandler.class,
        SecurityConfiguration.class, JwtConfiguration.class, ProblemDetailsSecurityHandler.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class SyncStatusControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    void returnsTheStateOfTheLastAttempt() throws Exception {
        when(catalogService.getSyncStatus()).thenReturn(SyncState.builder().localVersion(5)
                .lastSyncAt(Instant.parse("2026-10-27T19:54:25Z")).lastSyncType(SyncType.FULL)
                .lastStatus(SyncStatus.ERROR).lastError("CatedraUnavailableException: sin conexión").build());

        mockMvc.perform(get("/api/sync/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localVersion").value(5))
                .andExpect(jsonPath("$.lastSyncAt").value("2026-10-27T19:54:25Z"))
                .andExpect(jsonPath("$.lastSyncType").value("FULL"))
                .andExpect(jsonPath("$.lastStatus").value("ERROR"))
                .andExpect(jsonPath("$.lastError").value("CatedraUnavailableException: sin conexión"));
    }

    // Una base que nunca se sincronizó no es un error: version 0 y el resto sin valor, pero con la misma forma
    @Test
    void returnsVersionZeroAndNullsForABaseThatWasNeverSynchronized() throws Exception {
        when(catalogService.getSyncStatus()).thenReturn(SyncState.builder().build());

        mockMvc.perform(get("/api/sync/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localVersion").value(0))
                .andExpect(jsonPath("$.lastSyncAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lastSyncType").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lastStatus").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lastError").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(catalogService);
    }

    // Cualquier usuario autenticado puede consultarlo: no se pide ningún rol en particular
    @Test
    void anyAuthenticatedUserCanReadIt() throws Exception {
        when(catalogService.getSyncStatus()).thenReturn(SyncState.builder().localVersion(1).build());

        mockMvc.perform(get("/api/sync/status").with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAllowsGet() throws Exception {
        mockMvc.perform(post("/api/sync/status").with(jwt()))
                .andExpect(status().isMethodNotAllowed());
    }

    // Si la base fue alterada, no se cuentan detalles internos: 500 genérico y el motivo queda en el log
    @Test
    void answersWithAnInternalErrorWithoutDetailsWhenTheRowIsMissing() throws Exception {
        when(catalogService.getSyncStatus()).thenThrow(new SyncStateMissingException());

        mockMvc.perform(get("/api/sync/status").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("sync_state"))));
    }
}
