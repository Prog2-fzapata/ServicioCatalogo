package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;

// Un profesional con el nombre de su categoría y sus horarios semanales habilitados: lo que muestra una lista
public record ProfessionalSummary(Professional professional, String categoryName,
        List<WeeklySchedule> weeklySchedules) {
}
