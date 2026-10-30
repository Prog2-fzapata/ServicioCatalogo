package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.ProfessionalCategoryResponse;

class CategoryDtoMapperTest {

    private final CategoryDtoMapper mapper = new CategoryDtoMapper();

    @Test
    void mapsTheFieldsOfTheContract() {
        ProfessionalCategory category = category(3);
        category.setEnabled(false);

        ProfessionalCategoryResponse response = mapper.toResponse(category);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("Categoria 3");
        assertThat(response.getDescription()).isEqualTo("Descripcion 3");
        assertThat(response.isEnabled()).isFalse();
    }

    @Test
    void mapsNullToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
