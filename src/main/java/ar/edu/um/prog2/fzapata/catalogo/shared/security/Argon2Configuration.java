package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration
@EnableConfigurationProperties(Argon2Properties.class)
public class Argon2Configuration {

    // Tamaños del formato, no del costo: sal de 16 bytes y hash de 32 bytes
    static final int SALT_LENGTH = 16;
    static final int HASH_LENGTH = 32;

    @Bean
    Argon2PasswordEncoder argon2PasswordEncoder(Argon2Properties properties) {
        return new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, properties.parallelism(),
                properties.memoryKib(), properties.iterations());
    }
}
