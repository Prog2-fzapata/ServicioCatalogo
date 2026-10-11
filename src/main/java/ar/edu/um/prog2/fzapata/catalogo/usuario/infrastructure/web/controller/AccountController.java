package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtClaims;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.service.UserService;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.AccountResponse;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.AccountUpdateRequest;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.UserRegistrationRequest;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.mapper.AccountDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final AccountDtoMapper accountDtoMapper;

    @Operation(summary = "Registra un usuario nuevo; queda activo de inmediato")
    @PostMapping("/register")
    public ResponseEntity<Void> registerAccount(@Valid @RequestBody UserRegistrationRequest request) {
        userService.registerUser(accountDtoMapper.toDomain(request), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Devuelve la cuenta del usuario autenticado")
    @GetMapping("/account")
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountDtoMapper.toResponse(userService.getAccount(userId(jwt))));
    }

    @Operation(summary = "Actualiza los datos editables de la cuenta del usuario autenticado")
    @PostMapping("/account")
    public ResponseEntity<Void> saveAccount(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AccountUpdateRequest request) {
        userService.updateAccount(userId(jwt), accountDtoMapper.toDomain(request));
        return ResponseEntity.ok().build();
    }

    // El usuario siempre sale del token, nunca del body ni de la URL
    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim(JwtClaims.USER_ID)).longValue();
    }
}
