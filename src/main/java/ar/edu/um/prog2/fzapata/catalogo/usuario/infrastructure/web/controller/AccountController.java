package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.service.UserService;
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
}
