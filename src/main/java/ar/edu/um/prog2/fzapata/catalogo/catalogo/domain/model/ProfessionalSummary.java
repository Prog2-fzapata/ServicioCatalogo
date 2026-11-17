package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;

// Un profesional con el nombre de su categoría, si esa categoría está habilitada y sus horarios semanales
// habilitados: lo que muestra una lista
public record ProfessionalSummary(Professional professional, String categoryName, boolean categoryEnabled,
        List<WeeklySchedule> weeklySchedules) {
}
