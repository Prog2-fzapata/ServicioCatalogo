package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.ProfessionalSummaryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.ProfessionalDtoMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.ProfessionalQueryMapper;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.PaginationHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

// Requiere un JWT válido, como todo lo que no es registro ni login. Se resuelve siempre sobre la copia local
@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final CatalogService catalogService;
    private final ProfessionalQueryMapper professionalQueryMapper;
    private final ProfessionalDtoMapper professionalDtoMapper;

    @Operation(summary = "Busca profesionales con filtros opcionales y paginación, sobre la copia local del catálogo")
    @GetMapping
    public ResponseEntity<List<ProfessionalSummaryResponse>> searchProfessionals(
            @Parameter(description = "Solo los de esta categoría") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Texto parcial sobre nombre y apellido, sin distinguir mayúsculas ni tildes")
            @RequestParam(required = false) String name,
            @Parameter(description = "true o false; sin valor devuelve habilitados y deshabilitados")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "true: tiene al menos un horario semanal habilitado; false: no tiene ninguno")
            @RequestParam(required = false) Boolean available,
            @ParameterObject Pageable pageable) {
        PagedResult<ProfessionalSummary> result = catalogService.searchProfessionals(
                professionalQueryMapper.toFilter(categoryId, name, enabled, available),
                professionalQueryMapper.toPageQuery(pageable),
                professionalQueryMapper.toSortOrder(pageable));
        List<ProfessionalSummaryResponse> body = result.content().stream().map(professionalDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok()
                .headers(PaginationHeaders.create(ServletUriComponentsBuilder.fromCurrentRequest(),
                        result.pageQuery().page(), result.pageQuery().size(), result.totalElements()))
                .body(body);
    }
}
