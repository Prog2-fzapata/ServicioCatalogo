package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashHasBcryptFormatAndDoesNotContainThePassword() {
        String hash = hasher.hash("clave123");

        assertThat(hash).startsWith("$2a$10$").hasSize(60).doesNotContain("clave123");
    }

    @Test
    void samePasswordProducesDifferentHashesBecauseOfTheSalt() {
        assertThat(hasher.hash("clave123")).isNotEqualTo(hasher.hash("clave123"));
    }

    @Test
    void matchesAcceptsTheRightPasswordOnly() {
        String hash = hasher.hash("clave123");

        assertThat(hasher.matches("clave123", hash)).isTrue();
        assertThat(hasher.matches("otra", hash)).isFalse();
    }

    @Test
    void passwordsOfUpTo100CharactersAreAcceptedAndEveryCharacterCounts() {
        // BCrypt a secas ignoraría o rechazaría lo que pasa del byte 72
        String longPassword = "a".repeat(72) + "x";
        String hash = hasher.hash(longPassword);

        assertThat(hasher.matches(longPassword, hash)).isTrue();
        assertThat(hasher.matches("a".repeat(72) + "y", hash)).isFalse();
    }

    @Test
    void multibytePasswordOf100CharactersIsAccepted() {
        // 100 ñ pesan 200 bytes en UTF-8
        String password = "ñ".repeat(100);
        String hash = hasher.hash(password);

        assertThat(hasher.matches(password, hash)).isTrue();
    }
}
