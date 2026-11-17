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
    // Trers objetos uno metido dentro del otro: HttpClient -> JdkClientHttpRequestFactory -> RestClient
    static RestClient.Builder catedraRestClientBuilder(CatedraProperties properties) {
        // HTTP/1.1 explícito: evita el intento de subir a HTTP/2 que hace el cliente de Java sobre http://
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // Si no se fija, el cliente de Java solicita pasar a HTTP/2 en cada pedido
                .connectTimeout(properties.connectTimeout()) // ConnectTimeout
                .build();

        // RestClient es de Spring, HttpClient de Java. Esta clase los conecta. Algo que RestClient sabe usar.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout()); // ReadTimeout

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.jwt()) // JWT Técnico para todos los pedidos
                .requestFactory(requestFactory);
    }
}
