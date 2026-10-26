package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CatalogEventListenerTest {

    private static final String PAYLOAD = "{\"eventId\":\"evento-7\",\"eventType\":\"CatalogUpdated\",\"newVersion\":7}";
    private static final CatalogUpdatedEvent EVENT = new CatalogUpdatedEvent("evento-7", "CatalogUpdated", 7);

    @Mock
    private CatalogService catalogService;

    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    private CatalogEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CatalogEventListener(catalogService, new CatalogEventMapper(JsonMapper.builder().build()));
    }

    @Test
    void confirmsTheOffsetOnlyAfterTheEventWasHandled() {
        when(catalogService.synchronizeForEvent(EVENT)).thenReturn(Optional.of(new SyncResult(3, 7)));

        listener.onCatalogUpdated(PAYLOAD, acknowledgment);

        InOrder order = inOrder(catalogService, acknowledgment);
        order.verify(catalogService).synchronizeForEvent(EVENT);
        order.verify(acknowledgment).acknowledge();
    }

    @Test
    void confirmsTheOffsetOfAnIgnoredEventToo() {
        when(catalogService.synchronizeForEvent(EVENT)).thenReturn(Optional.empty());

        listener.onCatalogUpdated(PAYLOAD, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void confirmsTheOffsetWhenTheCatedraWentBackwards() {
        when(catalogService.synchronizeForEvent(EVENT)).thenReturn(Optional.of(new SyncResult(9, 7)));

        listener.onCatalogUpdated(PAYLOAD, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotConfirmTheOffsetWhenHandlingTheEventFails() {
        CatedraUnavailableException failure = new CatedraUnavailableException("sin conexión", null);
        when(catalogService.synchronizeForEvent(EVENT)).thenThrow(failure);

        assertThatThrownBy(() -> listener.onCatalogUpdated(PAYLOAD, acknowledgment)).isSameAs(failure);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void doesNotCallTheServiceNorConfirmWhenTheMessageIsInvalid() {
        assertThatThrownBy(() -> listener.onCatalogUpdated("no es json", acknowledgment))
                .isInstanceOf(InvalidCatalogEventException.class);

        verify(catalogService, never()).synchronizeForEvent(org.mockito.ArgumentMatchers.any());
        verify(acknowledgment, never()).acknowledge();
    }
}
