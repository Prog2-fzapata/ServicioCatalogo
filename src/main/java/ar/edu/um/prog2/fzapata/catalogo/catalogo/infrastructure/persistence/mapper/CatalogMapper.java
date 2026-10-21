package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.WeeklyScheduleEntity;

@Component
public class CatalogMapper {

    public ProfessionalCategoryEntity toEntity(ProfessionalCategory category) {
        if (category == null) return null;
        return ProfessionalCategoryEntity.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .enabled(category.isEnabled())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public ProfessionalEntity toEntity(Professional professional) {
        if (professional == null) return null;
        return ProfessionalEntity.builder()
                .id(professional.getId())
                .categoryId(professional.getCategoryId())
                .firstName(professional.getFirstName())
                .lastName(professional.getLastName())
                .enabled(professional.isEnabled())
                .createdAt(professional.getCreatedAt())
                .updatedAt(professional.getUpdatedAt())
                .build();
    }

    public WeeklyScheduleEntity toEntity(WeeklySchedule schedule) {
        if (schedule == null) return null;
        return WeeklyScheduleEntity.builder()
                .id(schedule.getId())
                .professionalId(schedule.getProfessionalId())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .enabled(schedule.isEnabled())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
