package ar.edu.um.prog2.fzapata.catalogo.catalogo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;

// Datos de ejemplo para los tests del catálogo
public final class CatalogFixtures {

    // Sin fracciones de segundo: la base guarda microsegundos y así lo que se lee es igual a lo que se escribió
    public static final Instant CREATED = Instant.parse("2026-01-10T12:00:00Z");
    public static final Instant UPDATED = Instant.parse("2026-02-20T08:30:00Z");

    private CatalogFixtures() {
    }

    public static ProfessionalCategory category(long id) {
        return ProfessionalCategory.builder().id(id).name("Categoria " + id).description("Descripcion " + id)
                .enabled(true).createdAt(CREATED).updatedAt(UPDATED).build();
    }

    public static Professional professional(long id, long categoryId) {
        return Professional.builder().id(id).categoryId(categoryId).firstName("Nombre" + id)
                .lastName("Apellido" + id).enabled(true).createdAt(CREATED).updatedAt(UPDATED).build();
    }

    public static WeeklySchedule schedule(long id, long professionalId) {
        return WeeklySchedule.builder().id(id).professionalId(professionalId).dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 30)).slotDurationMinutes(30)
                .enabled(true).createdAt(CREATED).updatedAt(UPDATED).build();
    }

    public static CatalogSnapshot snapshot(long version, List<ProfessionalCategory> categories,
            List<Professional> professionals, List<WeeklySchedule> schedules) {
        return CatalogSnapshot.builder().version(version).generatedAt(UPDATED).categories(categories)
                .professionals(professionals).weeklySchedules(schedules).build();
    }
}
