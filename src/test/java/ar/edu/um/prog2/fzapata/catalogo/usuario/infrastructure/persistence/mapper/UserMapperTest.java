package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.entity.UserEntity;

class UserMapperTest {

    private static final Instant CREATED = Instant.parse("2026-10-10T12:00:00Z");
    private static final Instant MODIFIED = Instant.parse("2026-10-11T13:30:00Z");

    private final UserMapper mapper = new UserMapper();

    @Test
    void toEntityCopiesEveryField() {
        User user = fullUser();

        UserEntity entity = mapper.toEntity(user);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getLogin()).isEqualTo("maria.perez");
        assertThat(entity.getPasswordHash()).isEqualTo("hash");
        assertThat(entity.getFirstName()).isEqualTo("Maria");
        assertThat(entity.getLastName()).isEqualTo("Perez");
        assertThat(entity.getEmail()).isEqualTo("maria@mail.com");
        assertThat(entity.getImageUrl()).isEqualTo("https://img.example/maria.png");
        assertThat(entity.getLangKey()).isEqualTo("es");
        assertThat(entity.isActivated()).isTrue();
        assertThat(entity.getAuthorities()).containsExactly("ROLE_USER");
        assertThat(entity.getCreatedDate()).isEqualTo(CREATED);
        assertThat(entity.getLastModifiedDate()).isEqualTo(MODIFIED);
    }

    @Test
    void toDomainModelCopiesEveryField() {
        UserEntity entity = mapper.toEntity(fullUser());

        User user = mapper.toDomainModel(entity);

        assertThat(user).usingRecursiveComparison().isEqualTo(fullUser());
    }

    @Test
    void authoritiesAreCopiedNotShared() {
        User user = fullUser();

        UserEntity entity = mapper.toEntity(user);
        entity.getAuthorities().add("ROLE_ADMIN");

        assertThat(user.getAuthorities()).containsExactly("ROLE_USER");
    }

    @Test
    void nullAuthoritiesBecomeEmptySet() {
        User user = User.builder().login("x").build();

        assertThat(mapper.toEntity(user).getAuthorities()).isEmpty();
        assertThat(mapper.toDomainModel(UserEntity.builder().login("x").build()).getAuthorities()).isEmpty();
    }

    @Test
    void nullInputReturnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomainModel(null)).isNull();
    }

    private User fullUser() {
        return User.builder()
                .id(7L)
                .login("maria.perez")
                .passwordHash("hash")
                .firstName("Maria")
                .lastName("Perez")
                .email("maria@mail.com")
                .imageUrl("https://img.example/maria.png")
                .langKey("es")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .createdDate(CREATED)
                .lastModifiedDate(MODIFIED)
                .build();
    }
}
