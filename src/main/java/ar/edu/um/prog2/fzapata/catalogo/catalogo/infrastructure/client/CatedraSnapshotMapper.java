package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatedraSnapshotMapper {

    private final CatedraEntityMapper entityMapper;

    private <T, R> List<R> mapAll(List<T> items, Function<T, R> mapper) {
        return items.stream().map(mapper).toList();
    }

    // Un snapshot incompleto no se aplica a medias: cualquier dato obligatorio ausente lo rechaza entero
    public CatalogSnapshot toDomainModel(CatedraSnapshotResponse response) {
        if (response == null) {
            throw entityMapper.invalid("la respuesta no tiene cuerpo");
        }
        return CatalogSnapshot.builder()
                .version(entityMapper.required(response.snapshotVersion(), "snapshotVersion"))
                .generatedAt(entityMapper.required(response.generatedAt(), "generatedAt"))
                .categories(mapAll(entityMapper.required(response.professionalCategories(), "professionalCategories"),
                        entityMapper::toDomainModel))
                .professionals(mapAll(entityMapper.required(response.professionals(), "professionals"),
                        entityMapper::toDomainModel))
                .weeklySchedules(mapAll(entityMapper.required(response.weeklySchedules(), "weeklySchedules"),
                        entityMapper::toDomainModel))
                .build();
    }
}
