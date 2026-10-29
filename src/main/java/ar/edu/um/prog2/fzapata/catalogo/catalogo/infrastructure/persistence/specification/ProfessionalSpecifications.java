package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.specification;

import org.springframework.data.jpa.domain.Specification;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.WeeklyScheduleEntity;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Subquery;

// Un criterio por método; la búsqueda los combina con AND solo si el pedido trae ese criterio
public final class ProfessionalSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ProfessionalSpecifications() {
    }

    // % y _ son comodines de LIKE: sin escaparlos, buscar «50%» o «a_b» encontraría cosas que no son
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<ProfessionalEntity> inCategory(long categoryId) {
        return (root, query, builder) -> builder.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<ProfessionalEntity> hasEnabled(boolean enabled) {
        return (root, query, builder) -> builder.equal(root.get("enabled"), enabled);
    }

    // Texto parcial sobre «nombre apellido» juntos, sin distinguir mayúsculas ni tildes (extensión unaccent):
    // «ana gom» encuentra a «Ana Gómez»
    public static Specification<ProfessionalEntity> nameContains(String text) {
        return (root, query, builder) -> {
            Expression<String> fullName = builder.concat(builder.concat(root.get("firstName"), " "),
                    root.get("lastName"));
            Expression<String> normalizedName = builder.lower(builder.function("unaccent", String.class, fullName));
            Expression<String> normalizedText = builder.lower(builder.function("unaccent", String.class,
                    builder.literal("%" + escapeLike(text) + "%")));
            return builder.like(normalizedName, normalizedText, LIKE_ESCAPE);
        };
    }

    // «Disponible» es tener al menos un horario semanal habilitado (decisión del usuario, A2): se mira la copia local
    // y no las ocupaciones reales, que son de la cátedra. Con false, los que no tienen ninguno
    public static Specification<ProfessionalEntity> hasEnabledSchedule(boolean expected) {
        return (root, query, builder) -> {
            Subquery<Long> schedules = query.subquery(Long.class);
            var schedule = schedules.from(WeeklyScheduleEntity.class);
            schedules.select(schedule.get("id")).where(
                    builder.equal(schedule.get("professionalId"), root.get("id")),
                    builder.isTrue(schedule.get("enabled")));
            return expected ? builder.exists(schedules) : builder.not(builder.exists(schedules));
        };
    }
}
