package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.category;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.professional;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;

class CatalogMapperTest {

    private final CatalogMapper mapper = new CatalogMapper();

    @Test
    void aCategoryGoesToTheEntityAndBackWithoutLosingAnyField() {
        ProfessionalCategory original = category(1);

        ProfessionalCategory roundTrip = mapper.toDomainModel(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void aProfessionalGoesToTheEntityAndBackWithoutLosingAnyField() {
        Professional original = professional(10, 1);

        Professional roundTrip = mapper.toDomainModel(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void aScheduleGoesToTheEntityAndBackWithoutLosingAnyField() {
        WeeklySchedule original = schedule(100, 10);

        WeeklySchedule roundTrip = mapper.toDomainModel(mapper.toEntity(original));

        assertThat(roundTrip).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void nullGoesToNullInBothDirections() {
        assertThat(mapper.toEntity((ProfessionalCategory) null)).isNull();
        assertThat(mapper.toEntity((Professional) null)).isNull();
        assertThat(mapper.toEntity((WeeklySchedule) null)).isNull();
        assertThat(mapper.toDomainModel((ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalCategoryEntity) null)).isNull();
        assertThat(mapper.toDomainModel((ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity) null)).isNull();
        assertThat(mapper.toDomainModel((ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.WeeklyScheduleEntity) null)).isNull();
    }
}
