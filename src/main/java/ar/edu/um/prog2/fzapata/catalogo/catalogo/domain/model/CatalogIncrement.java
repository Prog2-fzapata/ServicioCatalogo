package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que cambió en una versión: solo las entidades afectadas, con su estado vigente (incluidas las deshabilitadas)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogIncrement {

    private long version;
    private List<ProfessionalCategory> categories;
    private List<Professional> professionals;
    private List<WeeklySchedule> weeklySchedules;
}
