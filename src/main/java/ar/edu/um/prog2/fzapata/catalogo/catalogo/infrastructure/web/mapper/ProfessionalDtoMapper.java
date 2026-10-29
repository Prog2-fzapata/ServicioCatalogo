package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSummary;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.ProfessionalSummaryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.WeeklyScheduleSummaryResponse;

@Component
public class ProfessionalDtoMapper {

    private WeeklyScheduleSummaryResponse toResponse(WeeklySchedule schedule) {
        return WeeklyScheduleSummaryResponse.builder()
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .build();
    }

    public ProfessionalSummaryResponse toResponse(ProfessionalSummary summary) {
        if (summary == null) return null;
        return ProfessionalSummaryResponse.builder()
                .id(summary.professional().getId())
                .categoryId(summary.professional().getCategoryId())
                .categoryName(summary.categoryName())
                .firstName(summary.professional().getFirstName())
                .lastName(summary.professional().getLastName())
                .enabled(summary.professional().isEnabled())
                .weeklySchedules(summary.weeklySchedules().stream().map(this::toResponse).toList())
                .build();
    }
}
