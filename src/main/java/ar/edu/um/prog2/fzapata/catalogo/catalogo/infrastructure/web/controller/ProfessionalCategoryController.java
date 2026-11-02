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
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.ProfessionalCategoryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.CategoryDtoMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper.CategoryQueryMapper;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.PaginationHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

// Requiere un JWT válido, como todo lo que no es registro ni login. Se resuelve siempre sobre la copia local
@RestController
@RequestMapping("/api/professional-categories")
@RequiredArgsConstructor
public class ProfessionalCategoryController {

    private final CatalogService catalogService;
    private final CategoryQueryMapper categoryQueryMapper;
    private final CategoryDtoMapper categoryDtoMapper;

    @Operation(summary = "Lista las categorías con filtro opcional y paginación, sobre la copia local del catálogo")
    @GetMapping
    public ResponseEntity<List<ProfessionalCategoryResponse>> searchCategories(
            @Parameter(description = "true o false; sin valor devuelve habilitadas y deshabilitadas")
            @RequestParam(required = false) Boolean enabled,
            @ParameterObject Pageable pageable) {
        PagedResult<ProfessionalCategory> result = catalogService.searchCategories(
                categoryQueryMapper.toFilter(enabled),
                categoryQueryMapper.toPageQuery(pageable),
                categoryQueryMapper.toSortOrder(pageable));
        List<ProfessionalCategoryResponse> body = result.content().stream().map(categoryDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok()
                .headers(PaginationHeaders.create(ServletUriComponentsBuilder.fromCurrentRequest(),
                        result.pageQuery().page(), result.pageQuery().size(), result.totalElements()))
                .body(body);
    }
}
