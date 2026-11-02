package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Aparte de CatedraClientConfiguration: los nombres del contrato los usan el cliente REST y el de Redis
@Configuration
@EnableConfigurationProperties(CatedraContractProperties.class)
public class CatedraContractConfiguration {
}
