package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProcessedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.ProcessedEventMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProcessedEventRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaProcessedEventRepositoryAdapter implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpaProcessedEventRepository;
    private final ProcessedEventMapper processedEventMapper;

    @Override
    public boolean existsById(String eventId) {
        return jpaProcessedEventRepository.existsById(eventId);
    }

    @Override
    public void save(ProcessedEvent event) {
        jpaProcessedEventRepository.save(processedEventMapper.toEntity(event));
    }
}
