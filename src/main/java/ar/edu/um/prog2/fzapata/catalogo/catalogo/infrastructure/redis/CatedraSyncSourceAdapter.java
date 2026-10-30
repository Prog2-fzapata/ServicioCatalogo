package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraSyncSource;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.CatedraEntityMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.CategoryResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.ProfessionalResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.dto.CatedraSnapshotResponse.WeeklyScheduleResponse;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis.dto.CatedraChangesResponse;
import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraContractProperties;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Solo lectura: la ACL de la cuenta técnica permite leer catedra:sync:* y este adapter nunca escribe
@Component
@RequiredArgsConstructor
public class CatedraSyncSourceAdapter implements CatedraSyncSource {

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;
    private final CatedraEntityMapper entityMapper;
    private final RedisSyncMapper syncMapper;
    private final CatedraContractProperties contract;

    private CatedraContractProperties.RedisKeys keys() {
        return contract.redisKeys();
    }

    // Sin conexión, sin respuesta a tiempo o credenciales rechazadas: no hay dato que interpretar
    private <T> T redis(Supplier<T> read) {
        try {
            return read.get();
        } catch (DataAccessException e) {
            throw new CatedraUnavailableException("No se pudo leer Redis de la cátedra", e);
        }
    }

    private <T> T parse(String json, Class<T> type, String key) {
        try {
            return jsonMapper.readValue(json, type);
        } catch (JacksonException e) {
            throw new CatedraResponseException("El valor de " + key + " no se pudo leer como JSON", e);
        }
    }

    private String present(Object value, String key, String id) {
        if (value == null) {
            throw entityMapper.invalid("falta el id " + id + " en " + key);
        }
        return value.toString();
    }

    // El field del hash es el id decimal y el valor es el JSON de la entidad
    private <D, E> List<E> readByIds(String key, Collection<Long> ids, Class<D> type, Function<D, E> toDomain) {
        List<String> fields = ids.stream().distinct().map(String::valueOf).toList();
        List<Object> values = redis(() -> redisTemplate.opsForHash().multiGet(key, List.<Object>copyOf(fields)));
        return IntStream.range(0, fields.size())
                .mapToObj(i -> toDomain.apply(parse(present(values.get(i), key, fields.get(i)), type, key)))
                .toList();
    }

    @Override
    public SyncMetadata readMetadata() {
        return syncMapper.toMetadata(redis(() -> redisTemplate.opsForHash().entries(keys().metadata())));
    }

    @Override
    public Optional<CatalogChanges> readChanges(long version) {
        String key = keys().changesPrefix() + version;
        String json = redis(() -> redisTemplate.opsForValue().get(key));
        if (json == null) {
            return Optional.empty();
        }
        CatalogChanges changes = syncMapper.toChanges(parse(json, CatedraChangesResponse.class, key));
        // Una clave que trae otra versión no sirve para aplicar esta: se rechaza en lugar de aplicarla igual
        if (changes.version() != version) {
            throw entityMapper.invalid(key + " trae la versión " + changes.version());
        }
        return Optional.of(changes);
    }

    @Override
    public List<ProfessionalCategory> readCategories(Collection<Long> ids) {
        return readByIds(keys().categories(), ids, CategoryResponse.class, entityMapper::toDomainModel);
    }

    @Override
    public List<Professional> readProfessionals(Collection<Long> ids) {
        return readByIds(keys().professionals(), ids, ProfessionalResponse.class, entityMapper::toDomainModel);
    }

    @Override
    public List<WeeklySchedule> readWeeklySchedules(Collection<Long> ids) {
        return readByIds(keys().weeklySchedules(), ids, WeeklyScheduleResponse.class, entityMapper::toDomainModel);
    }
}
