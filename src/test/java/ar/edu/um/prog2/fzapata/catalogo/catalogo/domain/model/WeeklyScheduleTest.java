package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.CatalogFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeeklyScheduleTest {

    private WeeklySchedule scheduleOn(long id, DayOfWeek day, int startHour) {
        WeeklySchedule schedule = schedule(id, 10);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(startHour, 0));
        return schedule;
    }

    // El orden de DayOfWeek es el del calendario, no el alfabético: FRIDAY va después de MONDAY
    @Test
    void sortsByDayOfTheWeekAndThenByStartTime() {
        List<WeeklySchedule> sorted = List.of(scheduleOn(1, DayOfWeek.FRIDAY, 8), scheduleOn(2, DayOfWeek.MONDAY, 14),
                scheduleOn(3, DayOfWeek.MONDAY, 9), scheduleOn(4, DayOfWeek.WEDNESDAY, 9)).stream()
                .sorted(WeeklySchedule.BY_DAY_AND_START_TIME).toList();

        assertThat(sorted).extracting(WeeklySchedule::getId).containsExactly(3L, 2L, 4L, 1L);
    }
}
