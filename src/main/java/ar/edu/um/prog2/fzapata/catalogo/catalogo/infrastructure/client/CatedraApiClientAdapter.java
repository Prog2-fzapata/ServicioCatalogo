package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraAuthenticationException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatedraApiClientAdapter implements CatedraApiClient {

    private static final String SNAPSHOT_PATH = "/api/synchronization/snapshot";

    private final RestClient catedraRestClient;
    private final CatedraSnapshotMapper snapshotMapper;

    @Override
    public CatalogSnapshot fetchSnapshot() {
        try {
            CatedraSnapshotResponse response = catedraRestClient.get()
                    .uri(SNAPSHOT_PATH)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                        throw translate(httpResponse.getStatusCode());
                    })
                    .body(CatedraSnapshotResponse.class);
            return snapshotMapper.toDomainModel(response);
        } catch (ResourceAccessException e) {
            // Sin conexión o timeout: no hay respuesta que interpretar
            throw new CatedraUnavailableException("No se pudo conectar con la cátedra", e);
        } catch (RestClientException e) {
            // Llegó una respuesta, pero su cuerpo no se pudo leer como JSON del contrato
            throw new CatedraResponseException("La respuesta de la cátedra no se pudo leer", e);
        }
    }

    // Se decide solo por el status, nunca por el texto del detalle (lo pide el contrato); el mensaje no incluye el cuerpo
    private RuntimeException translate(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403) {
            return new CatedraAuthenticationException(
                    "La cátedra rechazó el JWT técnico (" + status.value() + "): revisar CATEDRA_JWT", null);
        }
        if (status.is5xxServerError()) {
            return new CatedraUnavailableException("La cátedra no está disponible (" + status.value() + ")", null);
        }
        return new CatedraResponseException("La cátedra respondió " + status.value(), null);
    }
}
