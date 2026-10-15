package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.CategoryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.ProfessionalResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.WeeklyScheduleResponse;

@Component
public class CatedraSnapshotMapper {

    // Un snapshot incompleto no se aplica a medias: cualquier dato obligatorio ausente lo rechaza entero
    public CatalogSnapshot toDomainModel(CatedraSnapshotResponse response) {
        if (response == null) {
            throw invalid("la respuesta no tiene cuerpo");
        }
        return CatalogSnapshot.builder()
                .version(required(response.snapshotVersion(), "snapshotVersion"))
                .generatedAt(required(response.generatedAt(), "generatedAt"))
                .categories(mapAll(required(response.professionalCategories(), "professionalCategories"), this::toDomainModel))
                .professionals(mapAll(required(response.professionals(), "professionals"), this::toDomainModel))
                .weeklySchedules(mapAll(required(response.weeklySchedules(), "weeklySchedules"), this::toDomainModel))
                .build();
    }

    private ProfessionalCategory toDomainModel(CategoryResponse category) {
        return ProfessionalCategory.builder()
                .id(required(category.id(), "professionalCategories.id"))
                .name(required(category.name(), "professionalCategories.name"))
                .description(category.description())
                .enabled(required(category.enabled(), "professionalCategories.enabled"))
                .createdAt(required(category.createdAt(), "professionalCategories.createdAt"))
                .updatedAt(required(category.updatedAt(), "professionalCategories.updatedAt"))
                .build();
    }

    private Professional toDomainModel(ProfessionalResponse professional) {
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

    private WeeklySchedule toDomainModel(WeeklyScheduleResponse schedule) {
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

    private <T, R> List<R> mapAll(List<T> items, Function<T, R> mapper) {
        return items.stream().map(mapper).toList();
    }

    private <T> T required(T value, String field) {
        if (value == null) {
            throw invalid("falta el campo " + field);
        }
        return value;
    }

    private CatedraResponseException invalid(String reason) {
        return new CatedraResponseException("Snapshot de la cátedra inválido: " + reason, null);
    }
}
