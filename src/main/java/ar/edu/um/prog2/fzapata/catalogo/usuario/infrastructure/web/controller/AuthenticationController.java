package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.service.UserService;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.JwtTokenResponse;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;

    @Operation(summary = "Autentica con login o email y devuelve el JWT")
    @PostMapping("/authenticate")
    public ResponseEntity<JwtTokenResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        String jwt = userService.authenticateUser(request.getUsername(), request.getPassword(),
                // Trampa de Jackson 3, false siempre que no sea true explicito
                Boolean.TRUE.equals(request.getRememberMe()));
        // El token va en el cuerpo y también en la cabecera Authorization, como JHipster
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .body(new JwtTokenResponse(jwt));
    }
}
