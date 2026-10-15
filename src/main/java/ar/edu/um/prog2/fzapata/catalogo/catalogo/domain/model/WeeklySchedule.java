package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySchedule {

    private Long id;
    private Long professionalId;
    private DayOfWeek dayOfWeek;
    // Horas locales de atención, sin zona horaria
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
