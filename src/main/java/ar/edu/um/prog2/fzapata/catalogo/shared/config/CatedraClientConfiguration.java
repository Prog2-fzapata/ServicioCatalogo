package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Configuración de conexión a la cátedra, común a los slices que la usen (REST hoy; Redis y Kafka después)
@Configuration
@EnableConfigurationProperties(CatedraProperties.class)
public class CatedraClientConfiguration {

    @Bean
    RestClient catedraRestClient(CatedraProperties properties) {
        return catedraRestClientBuilder(properties).build();
    }

    // Aparte del bean para que los tests puedan enganchar un servidor simulado al mismo builder
    static RestClient.Builder catedraRestClientBuilder(CatedraProperties properties) {
        // HTTP/1.1 explícito: evita el intento de subir a HTTP/2 que hace el cliente de Java sobre http://
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.jwt())
                .requestFactory(requestFactory);
    }
}
