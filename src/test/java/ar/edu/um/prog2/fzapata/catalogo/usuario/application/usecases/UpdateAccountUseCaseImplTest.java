package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateAccountUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    private UpdateAccountUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAccountUseCaseImpl(userRepository);
    }

    private User current() {
        return User.builder()
                .id(7L)
                .login("maria.perez")
                .passwordHash("hash")
                .firstName("Maria")
                .lastName("Perez")
                .email("maria@mail.com")
                .langKey("es")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();
    }

    private User changes(String email, String langKey) {
        return User.builder()
                .firstName("Mariana")
                .lastName("Gomez")
                .email(email)
                .imageUrl("https://img.example/m.png")
                .langKey(langKey)
                .build();
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(eq(7L), captor.capture());
        return captor.getValue();
    }

    @Test
    void updatesOnlyTheEditableFieldsAndKeepsLoginPasswordAndRoles() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(current()));
        when(userRepository.update(eq(7L), any(User.class)))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));

        useCase.updateAccount(7L, changes("Mariana@Mail.com", "en"));

        User saved = savedUser();
        assertThat(saved.getFirstName()).isEqualTo("Mariana");
        assertThat(saved.getLastName()).isEqualTo("Gomez");
        assertThat(saved.getEmail()).isEqualTo("mariana@mail.com");
        assertThat(saved.getImageUrl()).isEqualTo("https://img.example/m.png");
        assertThat(saved.getLangKey()).isEqualTo("en");
        assertThat(saved.getLogin()).isEqualTo("maria.perez");
        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getAuthorities()).containsExactly("ROLE_USER");
        assertThat(saved.isActivated()).isTrue();
    }

    @Test
    void blankLangKeyKeepsTheCurrentOne() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(current()));
        when(userRepository.update(eq(7L), any(User.class)))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));

        useCase.updateAccount(7L, changes("maria@mail.com", null));

        assertThat(savedUser().getLangKey()).isEqualTo("es");
    }

    @Test
    void keepingTheOwnEmailIsAllowed() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(current()));
        when(userRepository.findByEmail("maria@mail.com")).thenReturn(Optional.of(current()));
        when(userRepository.update(eq(7L), any(User.class)))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));

        assertThat(useCase.updateAccount(7L, changes("MARIA@mail.com", "es"))).isPresent();
    }

    @Test
    void emailOfAnotherUserIsRejected() {
        User other = current();
        other.setId(99L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(current()));
        when(userRepository.findByEmail("otra@mail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> useCase.updateAccount(7L, changes("otra@mail.com", "es")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).update(any(), any());
    }

    @Test
    void emailThatIsTheLoginOfAnotherUserIsRejected() {
        User other = current();
        other.setId(99L);
        other.setLogin("ana@mail.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(current()));
        when(userRepository.findByLogin("ana@mail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> useCase.updateAccount(7L, changes("Ana@Mail.com", "es")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).update(any(), any());
    }

    // Un usuario cuyo login tiene forma de email puede usar ese mismo texto como email
    @Test
    void emailThatIsTheUsersOwnLoginIsAllowed() {
        User owner = current();
        owner.setLogin("maria@mail.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(userRepository.findByLogin("maria@mail.com")).thenReturn(Optional.of(owner));
        when(userRepository.update(eq(7L), any(User.class)))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));

        assertThat(useCase.updateAccount(7L, changes("maria@mail.com", "es"))).isPresent();
    }

    @Test
    void missingUserReturnsEmpty() {
        assertThat(useCase.updateAccount(7L, changes("maria@mail.com", "es"))).isEmpty();

        verify(userRepository, never()).update(any(), any());
    }
}
