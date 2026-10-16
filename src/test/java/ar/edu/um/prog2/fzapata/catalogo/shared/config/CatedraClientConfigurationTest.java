package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CatedraClientConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CatedraClientConfiguration.class);

    private String rootMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private CatedraProperties properties(String baseUrl) {
        return new CatedraProperties(baseUrl, "jwt-secreto", Duration.ofSeconds(3), Duration.ofSeconds(10));
    }

    @Test
    void contextStartsWithTheRequiredSettings() {
        contextRunner.withPropertyValues("catedra.base-url=http://catedra.test", "catedra.jwt=abc",
                "catedra.connect-timeout=3s", "catedra.read-timeout=10s")
                .run(context -> assertThat(context).hasSingleBean(RestClient.class));
    }

    @Test
    void contextFailsWithoutTheBaseUrl() {
        contextRunner.withPropertyValues("catedra.jwt=abc", "catedra.connect-timeout=3s", "catedra.read-timeout=10s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextFailsWithABlankBaseUrl() {
        contextRunner.withPropertyValues("catedra.base-url= ", "catedra.jwt=abc", "catedra.connect-timeout=3s",
                "catedra.read-timeout=10s").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextFailsWithoutTheJwt() {
        contextRunner.withPropertyValues("catedra.base-url=http://catedra.test", "catedra.connect-timeout=3s",
                "catedra.read-timeout=10s").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextFailsWithABlankJwt() {
        contextRunner.withPropertyValues("catedra.base-url=http://catedra.test", "catedra.jwt= ",
                "catedra.connect-timeout=3s", "catedra.read-timeout=10s")
                .run(context -> assertThat(context).hasFailed());
    }

    // Con valor por defecto en application.properties esto no debería pasar nunca; si pasa, el error nombra la propiedad
    @Test
    void contextFailureNamesTheMissingTimeout() {
        contextRunner.withPropertyValues("catedra.base-url=http://catedra.test", "catedra.jwt=abc",
                "catedra.read-timeout=10s").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootMessages(context.getStartupFailure())).contains("connectTimeout");
                });
    }

    @Test
    void everyRequestCarriesTheTechnicalJwtAsBearer() {
        RestClient.Builder builder = CatedraClientConfiguration.catedraRestClientBuilder(properties("http://catedra.test"));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://catedra.test/ping"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt-secreto"))
                .andRespond(withSuccess());

        builder.build().get().uri("/ping").retrieve().toBodilessEntity();

        server.verify();
    }

    // El contrato entrega la URL sin "/" final; si llegara con uno, las rutas no deben quedar con "//"
    @Test
    void aTrailingSlashInTheBaseUrlDoesNotDuplicateIt() {
        RestClient.Builder builder = CatedraClientConfiguration.catedraRestClientBuilder(properties("http://catedra.test/"));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://catedra.test/ping")).andRespond(withSuccess());

        builder.build().get().uri("/ping").retrieve().toBodilessEntity();

        server.verify();
    }
}
