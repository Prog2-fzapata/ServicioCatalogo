package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.ProfessionalCategoryResponse;

@Component
public class CategoryDtoMapper {

    public ProfessionalCategoryResponse toResponse(ProfessionalCategory category) {
        if (category == null) return null;
        return ProfessionalCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .enabled(category.isEnabled())
                .build();
    }
}
