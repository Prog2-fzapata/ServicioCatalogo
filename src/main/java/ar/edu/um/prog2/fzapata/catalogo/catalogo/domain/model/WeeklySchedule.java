package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;

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

    // Lunes primero y, dentro de un día, por hora de inicio: así una agenda se lee de corrido
    public static final Comparator<WeeklySchedule> BY_DAY_AND_START_TIME = Comparator
            .comparing(WeeklySchedule::getDayOfWeek).thenComparing(WeeklySchedule::getStartTime);

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
