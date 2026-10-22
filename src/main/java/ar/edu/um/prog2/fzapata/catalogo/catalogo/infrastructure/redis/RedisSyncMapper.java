package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.CatedraEntityMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis.dto.CatedraChangesResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisSyncMapper {

    private final CatedraEntityMapper entityMapper;

    private String text(Map<Object, Object> hash, String field) {
        return String.valueOf(entityMapper.required(hash.get(field), field));
    }

    private long parseVersion(Map<Object, Object> hash, String field) {
        String text = text(hash, field);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw entityMapper.invalid("el campo " + field + " no es un número");
        }
    }

    private Instant parseInstant(Map<Object, Object> hash, String field) {
        String text = text(hash, field);
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            throw entityMapper.invalid("el campo " + field + " no es una fecha");
        }
    }

    // El hash metadata guarda todo como texto: se convierte acá y un valor ilegible rechaza la lectura entera
    public SyncMetadata toMetadata(Map<Object, Object> hash) {
        return new SyncMetadata(
                parseVersion(hash, "currentVersion"),
                parseVersion(hash, "oldestAvailableVersion"),
                parseInstant(hash, "publishedAt"));
    }

    public CatalogChanges toChanges(CatedraChangesResponse response) {
        CatedraChangesResponse.Changes changes = entityMapper.required(response.changes(), "changes");
        return new CatalogChanges(
                entityMapper.required(response.version(), "version"),
                entityMapper.required(changes.professionalCategories(), "changes.professionalCategories"),
                entityMapper.required(changes.professionals(), "changes.professionals"),
                entityMapper.required(changes.weeklySchedules(), "changes.weeklySchedules"));
    }
}
