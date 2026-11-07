package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.handler;

import java.sql.SQLException;

import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Se consulta antes que el handler compartido, que atrapa cualquier Exception
@RestControllerAdvice(basePackages = "ar.edu.um.prog2.fzapata.catalogo.usuario")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class UserExceptionHandler {

    private static final String LOGIN_CONSTRAINT = "uk_users_login";
    private static final String EMAIL_CONSTRAINT = "uk_users_email";

    private final ProblemDetailsFactory problemDetailsFactory;

    @ExceptionHandler(LoginAlreadyUsedException.class)
    public ResponseEntity<ProblemDetail> handleLoginAlreadyUsed(LoginAlreadyUsedException ex,
            HttpServletRequest request) {
        return problemDetailsFactory.response(HttpStatus.BAD_REQUEST, "LOGIN_ALREADY_USED", ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyUsed(EmailAlreadyUsedException ex,
            HttpServletRequest request) {
        return problemDetailsFactory.response(HttpStatus.BAD_REQUEST, "EMAIL_ALREADY_USED", ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex,
            HttpServletRequest request) {
        // Sin el login ni el email: el caso de uso no distingue por qué falló y el log tampoco debe hacerlo
        log.atWarn().setMessage("Credenciales rechazadas").addKeyValue("path", request.getRequestURI()).log();
        return problemDetailsFactory.response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return problemDetailsFactory.response(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage(),
                request.getRequestURI());
    }

    // Dos registros simultáneos pasan la verificación del caso de uso: la restricción única de la base frena al segundo
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (cause.contains(LOGIN_CONSTRAINT)) {
            return handleLoginAlreadyUsed(new LoginAlreadyUsedException(), request);
        }
        if (cause.contains(EMAIL_CONSTRAINT)) {
            return handleEmailAlreadyUsed(new EmailAlreadyUsedException(), request);
        }
        // El mensaje de PostgreSQL repite el valor que chocó (el login o el email): se registra solo la clase y el SQLSTATE
        Throwable root = ex.getMostSpecificCause();
        LoggingEventBuilder line = log.atError().setMessage("Violación de integridad no prevista")
                .addKeyValue("path", request.getRequestURI()).addKeyValue("exception", root.getClass().getSimpleName());
        if (root instanceof SQLException sqlException) {
            line = line.addKeyValue("sqlState", sqlException.getSQLState());
        }
        line.log();
        return problemDetailsFactory.internalError(request.getRequestURI());
    }
}
