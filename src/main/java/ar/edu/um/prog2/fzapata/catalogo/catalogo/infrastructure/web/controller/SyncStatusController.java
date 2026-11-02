package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.SyncStatusResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.SyncStatusDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

// Requiere un JWT válido, como todo lo que no es registro ni login: lo exige SecurityConfiguration
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncStatusController {

    private final CatalogService catalogService;
    private final SyncStatusDtoMapper syncStatusDtoMapper;

    @Operation(summary = "Devuelve el estado de la sincronización del catálogo: la versión local y el resultado del último intento")
    @GetMapping("/status")
    public ResponseEntity<SyncStatusResponse> getSyncStatus() {
        return ResponseEntity.ok(syncStatusDtoMapper.toResponse(catalogService.getSyncStatus()));
    }
}
