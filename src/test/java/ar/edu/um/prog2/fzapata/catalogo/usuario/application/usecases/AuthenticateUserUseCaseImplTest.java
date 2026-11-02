package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.TokenIssuer;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    private AuthenticateUserUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCaseImpl(userRepository, passwordHasher, tokenIssuer);
    }

    private User user(boolean activated) {
        return User.builder()
                .id(7L)
                .login("maria.perez")
                .email("maria@mail.com")
                .passwordHash("hash")
                .activated(activated)
                .authorities(Set.of("ROLE_USER"))
                .build();
    }

    private User userWith(long id, String login, String email, String passwordHash) {
        return User.builder()
                .id(id)
                .login(login)
                .email(email)
                .passwordHash(passwordHash)
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    void validCredentialsByLoginReturnTheToken() {
        User user = user(true);
        when(userRepository.findByLogin("maria.perez")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("clave123", "hash")).thenReturn(true);
        when(tokenIssuer.issueToken(user, false)).thenReturn("jwt");

        assertThat(useCase.authenticateUser("maria.perez", "clave123", false)).contains("jwt");
    }

    @Test
    void validCredentialsByEmailReturnTheToken() {
        User user = user(true);
        when(userRepository.findByLogin("maria@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("maria@mail.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("clave123", "hash")).thenReturn(true);
        when(tokenIssuer.issueToken(user, false)).thenReturn("jwt");

        assertThat(useCase.authenticateUser("maria@mail.com", "clave123", false)).contains("jwt");
    }

    @Test
    void usernameIsLookedUpInLowercase() {
        User user = user(true);
        when(userRepository.findByLogin("maria.perez")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("clave123", "hash")).thenReturn(true);
        when(tokenIssuer.issueToken(user, false)).thenReturn("jwt");

        assertThat(useCase.authenticateUser("MARIA.Perez", "clave123", false)).contains("jwt");
    }

    @Test
    void rememberMeIsPassedToTheIssuer() {
        User user = user(true);
        when(userRepository.findByLogin("maria.perez")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("clave123", "hash")).thenReturn(true);
        when(tokenIssuer.issueToken(user, true)).thenReturn("jwt-largo");

        assertThat(useCase.authenticateUser("maria.perez", "clave123", true)).contains("jwt-largo");
    }

    @Test
    void wrongPasswordReturnsEmptyAndIssuesNoToken() {
        when(userRepository.findByLogin("maria.perez")).thenReturn(Optional.of(user(true)));
        when(passwordHasher.matches("incorrecta", "hash")).thenReturn(false);

        assertThat(useCase.authenticateUser("maria.perez", "incorrecta", false)).isEmpty();

        verify(tokenIssuer, never()).issueToken(any(), anyBoolean());
    }

    @Test
    void unknownUserReturnsEmpty() {
        assertThat(useCase.authenticateUser("nadie", "clave123", false)).isEmpty();

        verifyNoInteractions(passwordHasher, tokenIssuer);
    }

    @Test
    void inactiveUserReturnsEmptyEvenWithTheRightPassword() {
        when(userRepository.findByLogin("maria.perez")).thenReturn(Optional.of(user(false)));

        assertThat(useCase.authenticateUser("maria.perez", "clave123", false)).isEmpty();

        verifyNoInteractions(passwordHasher, tokenIssuer);
    }

    // El texto es el login de un usuario y el email de otro: entra el dueño de la contraseña
    @Test
    void whenTheTextIsTheLoginOfOneUserAndTheEmailOfAnotherTheOneWhosePasswordMatchesEnters() {
        User loginOwner = userWith(1L, "ana@mail.com", "ana1@mail.com", "hash-login");
        User emailOwner = userWith(2L, "bruno", "ana@mail.com", "hash-email");
        when(userRepository.findByLogin("ana@mail.com")).thenReturn(Optional.of(loginOwner));
        when(userRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(emailOwner));
        when(passwordHasher.matches("clave-bruno", "hash-login")).thenReturn(false);
        when(passwordHasher.matches("clave-bruno", "hash-email")).thenReturn(true);
        when(tokenIssuer.issueToken(emailOwner, false)).thenReturn("jwt-bruno");

        assertThat(useCase.authenticateUser("ana@mail.com", "clave-bruno", false)).contains("jwt-bruno");
    }

    @Test
    void whenBothPasswordsWouldMatchTheLoginOwnerWinsAndTheOtherIsNotChecked() {
        User loginOwner = userWith(1L, "ana@mail.com", "ana1@mail.com", "hash-login");
        User emailOwner = userWith(2L, "bruno", "ana@mail.com", "hash-email");
        when(userRepository.findByLogin("ana@mail.com")).thenReturn(Optional.of(loginOwner));
        when(userRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(emailOwner));
        when(passwordHasher.matches("clave", "hash-login")).thenReturn(true);
        when(tokenIssuer.issueToken(loginOwner, false)).thenReturn("jwt-ana");

        assertThat(useCase.authenticateUser("ana@mail.com", "clave", false)).contains("jwt-ana");

        verify(passwordHasher, never()).matches(any(), eq("hash-email"));
    }

    @Test
    void whenNeitherPasswordMatchesNobodyEnters() {
        when(userRepository.findByLogin("ana@mail.com"))
                .thenReturn(Optional.of(userWith(1L, "ana@mail.com", "ana1@mail.com", "hash-login")));
        when(userRepository.findByEmail("ana@mail.com"))
                .thenReturn(Optional.of(userWith(2L, "bruno", "ana@mail.com", "hash-email")));

        assertThat(useCase.authenticateUser("ana@mail.com", "incorrecta", false)).isEmpty();

        verify(tokenIssuer, never()).issueToken(any(), anyBoolean());
    }

    // Login y email del mismo usuario coinciden: se verifica la contraseña una sola vez
    @Test
    void theSameUserFoundByLoginAndByEmailIsCheckedOnlyOnce() {
        when(userRepository.findByLogin("ana@mail.com"))
                .thenReturn(Optional.of(userWith(1L, "ana@mail.com", "ana@mail.com", "hash")));
        when(userRepository.findByEmail("ana@mail.com"))
                .thenReturn(Optional.of(userWith(1L, "ana@mail.com", "ana@mail.com", "hash")));

        assertThat(useCase.authenticateUser("ana@mail.com", "incorrecta", false)).isEmpty();

        verify(passwordHasher, times(1)).matches(any(), any());
    }
}
