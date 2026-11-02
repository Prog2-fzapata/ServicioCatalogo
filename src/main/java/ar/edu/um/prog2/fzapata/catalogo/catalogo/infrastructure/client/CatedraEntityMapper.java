package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.CategoryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.ProfessionalResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.WeeklyScheduleResponse;

// Convierte las entidades que publica la cátedra al dominio. Tienen la misma forma en el snapshot REST y en los
// hashes de Redis, así que las comparten los dos adapters
@Component
public class CatedraEntityMapper {

    public CatedraResponseException invalid(String reason) {
        return new CatedraResponseException("Dato de la cátedra inválido: " + reason, null);
    }

    // Un dato obligatorio ausente invalida lo que se estaba leyendo: no se aplica a medias
    public <T> T required(T value, String field) {
        if (value == null) {
            throw invalid("falta el campo " + field);
        }
        return value;
    }

    public ProfessionalCategory toDomainModel(CategoryResponse category) {
        return ProfessionalCategory.builder()
                .id(required(category.id(), "professionalCategories.id"))
                .name(required(category.name(), "professionalCategories.name"))
                .description(category.description())
                .enabled(required(category.enabled(), "professionalCategories.enabled"))
                .createdAt(required(category.createdAt(), "professionalCategories.createdAt"))
                .updatedAt(required(category.updatedAt(), "professionalCategories.updatedAt"))
                .build();
    }

    public Professional toDomainModel(ProfessionalResponse professional) {
        return Professional.builder()
                .id(required(professional.id(), "professionals.id"))
                .categoryId(required(professional.categoryId(), "professionals.categoryId"))
                .firstName(required(professional.firstName(), "professionals.firstName"))
                .lastName(required(professional.lastName(), "professionals.lastName"))
                .enabled(required(professional.enabled(), "professionals.enabled"))
                .createdAt(required(professional.createdAt(), "professionals.createdAt"))
                .updatedAt(required(professional.updatedAt(), "professionals.updatedAt"))
                .build();
    }

    public WeeklySchedule toDomainModel(WeeklyScheduleResponse schedule) {
        return WeeklySchedule.builder()
                .id(required(schedule.id(), "weeklySchedules.id"))
                .professionalId(required(schedule.professionalId(), "weeklySchedules.professionalId"))
                .dayOfWeek(required(schedule.dayOfWeek(), "weeklySchedules.dayOfWeek"))
                .startTime(required(schedule.startTime(), "weeklySchedules.startTime"))
                .endTime(required(schedule.endTime(), "weeklySchedules.endTime"))
                .slotDurationMinutes(required(schedule.slotDurationMinutes(), "weeklySchedules.slotDurationMinutes"))
                .enabled(required(schedule.enabled(), "weeklySchedules.enabled"))
                .createdAt(required(schedule.createdAt(), "weeklySchedules.createdAt"))
                .updatedAt(required(schedule.updatedAt(), "weeklySchedules.updatedAt"))
                .build();
    }
}
