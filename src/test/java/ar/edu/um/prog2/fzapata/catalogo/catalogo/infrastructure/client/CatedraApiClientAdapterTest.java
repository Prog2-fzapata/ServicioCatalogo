package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraAuthenticationException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;

class CatedraApiClientAdapterTest {

    private static final String SNAPSHOT_URL = "http://catedra.test/api/synchronization/snapshot";

    private MockRestServiceServer server;
    private CatedraApiClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catedra.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CatedraApiClientAdapter(builder.build(), new CatedraSnapshotMapper());
    }

    private String snapshotJson(String scheduleTimes, String extraFields) {
        return """
                {
                  "snapshotVersion": 7,
                  "generatedAt": "2026-09-10T13:44:59Z",
                  %s
                  "professionalCategories": [
                    {"id": 1, "name": "Clinica medica", "description": "Atencion clinica general", "enabled": true,
                     "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
                  ],
                  "professionals": [
                    {"id": 15, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez", "enabled": true,
                     "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
                  ],
                  "weeklySchedules": [
                    {"id": 21, "professionalId": 15, "dayOfWeek": "MONDAY", %s, "slotDurationMinutes": 30,
                     "enabled": false, "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}
                  ]
                }""".formatted(extraFields, scheduleTimes);
    }

    private String validSnapshotJson() {
        return snapshotJson("\"startTime\": \"09:00:00\", \"endTime\": \"12:00:00\"", "");
    }

    private void respondWith(HttpStatus status) {
        server.expect(requestTo(SNAPSHOT_URL)).andRespond(withStatus(status));
    }

    private void respondWithBody(String body) {
        server.expect(requestTo(SNAPSHOT_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void fetchSnapshotAsksForTheSnapshotWithGet() {
        server.expect(requestTo(SNAPSHOT_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(validSnapshotJson(), MediaType.APPLICATION_JSON));

        adapter.fetchSnapshot();

        server.verify();
    }

    @Test
    void fetchSnapshotMapsEverythingIncludingDisabledEntities() {
        respondWithBody(validSnapshotJson());

        CatalogSnapshot snapshot = adapter.fetchSnapshot();

        assertThat(snapshot.getVersion()).isEqualTo(7);
        assertThat(snapshot.getGeneratedAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
        assertThat(snapshot.getCategories()).singleElement().satisfies(category -> {
            assertThat(category.getId()).isEqualTo(1);
            assertThat(category.getName()).isEqualTo("Clinica medica");
            assertThat(category.getDescription()).isEqualTo("Atencion clinica general");
            assertThat(category.isEnabled()).isTrue();
            assertThat(category.getUpdatedAt()).isEqualTo(Instant.parse("2026-09-10T13:40:00Z"));
        });
        assertThat(snapshot.getProfessionals()).singleElement().satisfies(professional -> {
            assertThat(professional.getId()).isEqualTo(15);
            assertThat(professional.getCategoryId()).isEqualTo(1);
            assertThat(professional.getFirstName()).isEqualTo("Ana");
            assertThat(professional.getLastName()).isEqualTo("Gomez");
        });
        assertThat(snapshot.getWeeklySchedules()).singleElement().satisfies(schedule -> {
            assertThat(schedule.getProfessionalId()).isEqualTo(15);
            assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
            assertThat(schedule.isEnabled()).isFalse();
        });
    }

    // El contrato pide aceptar las dos formas
    @Test
    void fetchSnapshotAcceptsTimesWithAndWithoutSeconds() {
        respondWithBody(snapshotJson("\"startTime\": \"09:00\", \"endTime\": \"12:30:15\"", ""));

        WeeklySchedule schedule = adapter.fetchSnapshot().getWeeklySchedules().getFirst();

        assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 30, 15));
    }

    // Los clientes deben ignorar campos adicionales compatibles con v1
    @Test
    void fetchSnapshotIgnoresUnknownFields() {
        respondWithBody(snapshotJson("\"startTime\": \"09:00\", \"endTime\": \"12:00\", \"campoNuevo\": 1",
                "\"otroCampo\": \"x\","));

        assertThat(adapter.fetchSnapshot().getVersion()).isEqualTo(7);
    }

    @Test
    void fetchSnapshotAcceptsEmptyCollections() {
        respondWithBody("""
                {"snapshotVersion": 3, "generatedAt": "2026-09-10T13:44:59Z",
                 "professionalCategories": [], "professionals": [], "weeklySchedules": []}""");

        CatalogSnapshot snapshot = adapter.fetchSnapshot();

        assertThat(snapshot.getVersion()).isEqualTo(3);
        assertThat(snapshot.getCategories()).isEmpty();
        assertThat(snapshot.getProfessionals()).isEmpty();
        assertThat(snapshot.getWeeklySchedules()).isEmpty();
    }

    @Test
    void unauthorizedIsReportedAsAnAuthenticationFailure() {
        respondWith(HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraAuthenticationException.class)
                .hasMessageContaining("401");
    }

    @Test
    void forbiddenIsReportedAsAnAuthenticationFailure() {
        respondWith(HttpStatus.FORBIDDEN);

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraAuthenticationException.class)
                .hasMessageContaining("403");
    }

    @Test
    void serverErrorsAreReportedAsUnavailable() {
        respondWith(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void serviceUnavailableIsReportedAsUnavailable() {
        respondWith(HttpStatus.SERVICE_UNAVAILABLE);

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraUnavailableException.class)
                .hasMessageContaining("503");
    }

    @Test
    void connectionFailureIsReportedAsUnavailable() {
        server.expect(requestTo(SNAPSHOT_URL)).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraUnavailableException.class)
                .hasRootCauseInstanceOf(IOException.class);
    }

    @Test
    void otherClientErrorsAreReportedAsAnUnusableResponse() {
        respondWith(HttpStatus.NOT_FOUND);

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("404");
    }

    // El mensaje no copia el cuerpo de la cátedra: podría traer datos que no queremos en los logs
    @Test
    void errorMessageDoesNotIncludeTheResponseBody() {
        server.expect(requestTo(SNAPSHOT_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body("{\"detail\":\"dato-sensible\"}"));

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraResponseException.class)
                .hasMessageNotContaining("dato-sensible");
    }

    @Test
    void bodyThatIsNotJsonIsReportedAsAnUnusableResponse() {
        respondWithBody("esto no es json");

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraResponseException.class);
    }

    @Test
    void unknownDayOfWeekIsReportedAsAnUnusableResponse() {
        respondWithBody(validSnapshotJson().replace("MONDAY", "FUNDAY"));

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraResponseException.class);
    }

    @Test
    void emptyBodyIsReportedAsAnUnusableResponse() {
        server.expect(requestTo(SNAPSHOT_URL)).andRespond(withSuccess());

        assertThatThrownBy(adapter::fetchSnapshot).isInstanceOf(CatedraResponseException.class);
    }
}
