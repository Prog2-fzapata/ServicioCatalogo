package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;

// Lectura de los datos de sincronización que la cátedra publica. Todas las operaciones fallan con
// CatedraException (application.exception) si la cátedra no responde o publica algo inválido
public interface CatedraSyncSource {

    SyncMetadata readMetadata();

    // Vacío si la versión ya salió de la ventana que la cátedra retiene (hay que pedir un snapshot)
    Optional<CatalogChanges> readChanges(long version);

    // Estado vigente de cada id pedido; un id que la cátedra no publica es un fallo de contrato
    List<ProfessionalCategory> readCategories(Collection<Long> ids);

    List<Professional> readProfessionals(Collection<Long> ids);

    List<WeeklySchedule> readWeeklySchedules(Collection<Long> ids);
}
