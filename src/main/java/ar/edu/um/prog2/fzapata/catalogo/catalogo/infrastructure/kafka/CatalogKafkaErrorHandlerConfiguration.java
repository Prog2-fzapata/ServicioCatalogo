package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraKafkaProperties;

@Configuration
public class CatalogKafkaErrorHandlerConfiguration {

    private static final double BACKOFF_MULTIPLIER = 2.0;

    // Reintentos acotados con espera creciente. Agotados, el mensaje se descarta con un log de error y se
    // sigue (sin cola de eventos muertos, D70): si se pierde una notificación, la verificación periódica de
    // versión (CAT-12) la recupera. Un mensaje inválido se descarta sin reintentar
    @Bean
    DefaultErrorHandler catalogKafkaErrorHandler(CatedraKafkaProperties properties) {
        ExponentialBackOff backOff = new ExponentialBackOff(properties.initialBackoff().toMillis(), BACKOFF_MULTIPLIER);
        backOff.setMaxAttempts(properties.maxRetries());
        DefaultErrorHandler handler = new DefaultErrorHandler(backOff);
        handler.addNotRetryableExceptions(InvalidCatalogEventException.class);
        return handler;
    }
}
