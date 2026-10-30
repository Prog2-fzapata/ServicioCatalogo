package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetSyncStatusUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetSyncStatusUseCaseImpl implements GetSyncStatusUseCase {

    private final SyncStateRepository syncStateRepository;

    // Lectura sin bloqueo: consultar el estado no debe esperar a una sincronización en curso
    @Override
    public Optional<SyncState> getSyncStatus() {
        return syncStateRepository.find();
    }
}
