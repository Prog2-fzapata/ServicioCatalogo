package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.entity.UserEntity;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaUserRepositoryAdapter.class, UserMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JpaUserRepositoryAdapterTest {

    private final JpaUserRepositoryAdapter adapter;
    private final EntityManager entityManager;

    private User newUser(String login, String email) {
        return User.builder()
                .login(login)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234")
                .email(email)
                .langKey("es")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    void createPersistsUserWithGeneratedIdTimestampsAndAuthorities() {
        User created = adapter.create(newUser("maria.perez", "maria@mail.com"));
        // flush escribe en la base y clear vacía la memoria de Hibernate: lo que se lee viene de la base
        entityManager.flush();
        entityManager.clear();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedDate()).isNotNull();
        assertThat(created.getLastModifiedDate()).isNotNull();
        assertThat(created.getAuthorities()).containsExactly("ROLE_USER");
        assertThat(adapter.existsByLogin("maria.perez")).isTrue();
    }

    @Test
    void createStoresEveryFieldInItsColumn() {
        User toStore = User.builder()
                .login("sofia")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234")
                .firstName("Sofia")
                .lastName("Lopez")
                .email("sofia@mail.com")
                .imageUrl("https://img.example/sofia.png")
                .langKey("en")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();

        Long id = adapter.create(toStore).getId();
        entityManager.flush();
        entityManager.clear();

        UserEntity row = entityManager.find(UserEntity.class, id);
        assertThat(row.getLogin()).isEqualTo("sofia");
        assertThat(row.getPasswordHash()).isEqualTo(toStore.getPasswordHash());
        assertThat(row.getFirstName()).isEqualTo("Sofia");
        assertThat(row.getLastName()).isEqualTo("Lopez");
        assertThat(row.getEmail()).isEqualTo("sofia@mail.com");
        assertThat(row.getImageUrl()).isEqualTo("https://img.example/sofia.png");
        assertThat(row.getLangKey()).isEqualTo("en");
        assertThat(row.isActivated()).isTrue();
        assertThat(row.getAuthorities()).containsExactly("ROLE_USER");
    }

    @Test
    void existsByLoginAndEmailReflectStoredUsers() {
        adapter.create(newUser("juan.gomez", "juan@mail.com"));
        entityManager.flush();

        assertThat(adapter.existsByLogin("juan.gomez")).isTrue();
        assertThat(adapter.existsByLogin("otro")).isFalse();
        assertThat(adapter.existsByEmail("juan@mail.com")).isTrue();
        assertThat(adapter.existsByEmail("otro@mail.com")).isFalse();
    }

    @Test
    void createRejectsDuplicateLogin() {
        adapter.create(newUser("ana", "ana@mail.com"));
        entityManager.flush();

        User duplicated = newUser("ana", "ana2@mail.com");

        // El nombre de la restricción es el que usa el manejo de errores para distinguir login de email
        assertThatThrownBy(() -> adapter.create(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_users_login");
    }

    @Test
    void createRejectsDuplicateEmail() {
        adapter.create(newUser("luis", "luis@mail.com"));
        entityManager.flush();

        User duplicated = newUser("luis2", "luis@mail.com");

        assertThatThrownBy(() -> adapter.create(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_users_email");
    }
}
