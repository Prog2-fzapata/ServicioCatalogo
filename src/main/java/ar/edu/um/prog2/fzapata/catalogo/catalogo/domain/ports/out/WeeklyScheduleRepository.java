package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import java.util.Collection;
import java.util.List;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;

public interface WeeklyScheduleRepository {

    // Solo los habilitados, de todos los profesionales pedidos, en una sola consulta
    List<WeeklySchedule> findEnabledByProfessionalIds(Collection<Long> professionalIds);

    // Todos los de un profesional, también los deshabilitados
    List<WeeklySchedule> findByProfessionalId(Long professionalId);
}
