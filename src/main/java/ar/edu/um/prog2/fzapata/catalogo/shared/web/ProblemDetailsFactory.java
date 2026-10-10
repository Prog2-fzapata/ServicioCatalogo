package ar.edu.um.prog2.fzapata.catalogo.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailsFactory {

    public ProblemDetail of(HttpStatus status, String code, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("path", path);
        problem.setProperty("code", code);
        return problem;
    }

    public ResponseEntity<ProblemDetail> response(HttpStatus status, String code, String detail, String path) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(of(status, code, detail, path));
    }

    // El detalle es genérico a propósito: nunca se exponen mensajes internos al cliente
    public ResponseEntity<ProblemDetail> internalError(String path) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno del servidor", path);
    }
}
