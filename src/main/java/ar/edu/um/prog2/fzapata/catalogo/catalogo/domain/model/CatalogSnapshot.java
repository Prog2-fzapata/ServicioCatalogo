package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Copia completa del catálogo de la cátedra en una versión; incluye también las entidades deshabilitadas
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSnapshot {

    private long version;
    private Instant generatedAt;
    private List<ProfessionalCategory> categories;
    private List<Professional> professionals;
    private List<WeeklySchedule> weeklySchedules;
}
