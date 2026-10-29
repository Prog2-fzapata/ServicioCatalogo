package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.handler;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.exception.InvalidSearchParameterException;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.FieldViolation;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

// Se consulta antes que el handler compartido, que atrapa cualquier Exception
@RestControllerAdvice(basePackages = "ar.edu.um.prog2.fzapata.catalogo.catalogo")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CatalogExceptionHandler {

    private static final String SEARCH_OBJECT = "professionalSearch";

    private final ProblemDetailsFactory problemDetailsFactory;

    // Mismo formato que los errores de validación del body: VALIDATION_ERROR con fieldErrors
    @ExceptionHandler(InvalidSearchParameterException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSearchParameter(InvalidSearchParameterException ex,
            HttpServletRequest request) {
        ProblemDetail problem = problemDetailsFactory.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Los parámetros de búsqueda no son válidos", request.getRequestURI());
        problem.setProperty("fieldErrors", List.of(new FieldViolation(SEARCH_OBJECT, ex.getField(), ex.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
