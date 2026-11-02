package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private RegisterUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCaseImpl(userRepository, passwordHasher);
    }

    private User captureSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).create(captor.capture());
        return captor.getValue();
    }

    private User request(String login, String email, String langKey) {
        return User.builder()
                .login(login)
                .email(email)
                .firstName("Maria")
                .lastName("Perez")
                .langKey(langKey)
                .build();
    }

    @Test
    void registersActiveUserWithRoleUserHashedPasswordAndNormalizedLoginAndEmail() {
        // No se programa existsByLogin/existsByEmail: un doble sin programar responde false, o sea "no existe"
        when(passwordHasher.hash("clave123")).thenReturn("hashed");
        when(userRepository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.registerUser(request("Maria.Perez", "Maria@Mail.com", null), "clave123");

        User saved = captureSavedUser();
        assertThat(saved.getLogin()).isEqualTo("maria.perez");
        assertThat(saved.getEmail()).isEqualTo("maria@mail.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.isActivated()).isTrue();
        assertThat(saved.getAuthorities()).containsExactly("ROLE_USER");
        assertThat(saved.getLangKey()).isEqualTo("es");
        assertThat(saved.getFirstName()).isEqualTo("Maria");
        assertThat(saved.getLastName()).isEqualTo("Perez");
    }

    @Test
    void ignoresIdActivationAndAuthoritiesChosenByTheClient() {
        User request = request("maria.perez", "maria@mail.com", "en");
        request.setId(99L);
        request.setActivated(false);
        request.setAuthorities(Set.of("ROLE_ADMIN"));
        when(userRepository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.registerUser(request, "clave123");

        User saved = captureSavedUser();
        assertThat(saved.getId()).isNull();
        assertThat(saved.isActivated()).isTrue();
        assertThat(saved.getAuthorities()).containsExactly("ROLE_USER");
    }

    @Test
    void keepsTheLangKeyWhenProvidedAndDefaultsWhenBlank() {
        when(userRepository.create(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.registerUser(request("uno", "uno@mail.com", "en"), "clave123");
        useCase.registerUser(request("dos", "dos@mail.com", "  "), "clave123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).create(captor.capture());
        assertThat(captor.getAllValues()).extracting(User::getLangKey).containsExactly("en", "es");
    }

    @Test
    void rejectsAnAlreadyUsedLoginIgnoringCaseWithoutHashingOrSaving() {
        // El doble solo responde true al login en minúsculas: comprueba también la normalización
        when(userRepository.existsByLogin("maria.perez")).thenReturn(true);

        User duplicated = request("MARIA.PEREZ", "nuevo@mail.com", null);

        assertThatThrownBy(() -> useCase.registerUser(duplicated, "clave123"))
                .isInstanceOf(LoginAlreadyUsedException.class);

        verify(passwordHasher, never()).hash(any());
        verify(userRepository, never()).create(any());
    }

    @Test
    void rejectsAnAlreadyUsedEmailIgnoringCaseWithoutHashingOrSaving() {
        // Ídem para el email: el pedido llega con mayúsculas
        when(userRepository.existsByEmail("nuevo")).thenReturn(false);
        when(userRepository.existsByEmail("maria@mail.com")).thenReturn(true);

        User duplicated = request("nuevo", "MARIA@mail.com", null);

        assertThatThrownBy(() -> useCase.registerUser(duplicated, "clave123"))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(passwordHasher, never()).hash(any());
        verify(userRepository, never()).create(any());
    }

    // Un login no puede ser el email de otro usuario ni al revés: el texto de ingreso no queda ambiguo
    @Test
    void rejectsALoginThatIsTheEmailOfAnotherUser() {
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        User squatter = request("Ana@Mail.com", "otro@mail.com", null);

        assertThatThrownBy(() -> useCase.registerUser(squatter, "clave123"))
                .isInstanceOf(LoginAlreadyUsedException.class);

        verify(passwordHasher, never()).hash(any());
        verify(userRepository, never()).create(any());
    }

    @Test
    void rejectsAnEmailThatIsTheLoginOfAnotherUser() {
        when(userRepository.existsByLogin("nuevo")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo")).thenReturn(false);
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(userRepository.existsByLogin("ana@mail.com")).thenReturn(true);

        User newcomer = request("nuevo", "Ana@Mail.com", null);

        assertThatThrownBy(() -> useCase.registerUser(newcomer, "clave123"))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(passwordHasher, never()).hash(any());
        verify(userRepository, never()).create(any());
    }
}
