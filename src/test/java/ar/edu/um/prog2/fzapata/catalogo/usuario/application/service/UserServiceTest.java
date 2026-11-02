package ar.edu.um.prog2.fzapata.catalogo.usuario.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.UserNotFoundException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.AuthenticateUserUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.GetAccountUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.RegisterUserUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.UpdateAccountUseCase;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Mock
    private GetAccountUseCase getAccountUseCase;

    @Mock
    private UpdateAccountUseCase updateAccountUseCase;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(registerUserUseCase, authenticateUserUseCase, getAccountUseCase,
                updateAccountUseCase);
    }

    @Test
    void authenticateReturnsTheToken() {
        when(authenticateUserUseCase.authenticateUser("maria", "clave", true)).thenReturn(Optional.of("jwt"));

        assertThat(service.authenticateUser("maria", "clave", true)).isEqualTo("jwt");
    }

    @Test
    void authenticateWithoutTokenThrowsInvalidCredentials() {
        when(authenticateUserUseCase.authenticateUser("maria", "mala", false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticateUser("maria", "mala", false))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getAccountReturnsTheUser() {
        User user = User.builder().id(7L).build();
        when(getAccountUseCase.getAccount(7L)).thenReturn(Optional.of(user));

        assertThat(service.getAccount(7L)).isSameAs(user);
    }

    @Test
    void getAccountOfMissingUserThrowsNotFound() {
        when(getAccountUseCase.getAccount(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(7L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateAccountOfMissingUserThrowsNotFound() {
        User changes = User.builder().email("m@mail.com").build();
        when(updateAccountUseCase.updateAccount(7L, changes)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAccount(7L, changes)).isInstanceOf(UserNotFoundException.class);
    }
}
