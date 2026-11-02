package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProcessedEvent;

public interface ProcessedEventRepository {

    boolean existsById(String eventId);

    void save(ProcessedEvent event);
}
