package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

// Forma de GET /api/synchronization/snapshot. Spring ya ignora los campos desconocidos (lo pide el contrato v1);
// los que faltan quedan en null y el mapper los rechaza con un error claro
public record CatedraSnapshotResponse(
        Long snapshotVersion,
        Instant generatedAt,
        List<CategoryResponse> professionalCategories,
        List<ProfessionalResponse> professionals,
        List<WeeklyScheduleResponse> weeklySchedules) {

    public record CategoryResponse(Long id, String name, String description, Boolean enabled,
            Instant createdAt, Instant updatedAt) {
    }

    public record ProfessionalResponse(Long id, Long categoryId, String firstName, String lastName, Boolean enabled,
            Instant createdAt, Instant updatedAt) {
    }

    public record WeeklyScheduleResponse(Long id, Long professionalId, DayOfWeek dayOfWeek, LocalTime startTime,
            LocalTime endTime, Integer slotDurationMinutes, Boolean enabled, Instant createdAt, Instant updatedAt) {
    }
}
