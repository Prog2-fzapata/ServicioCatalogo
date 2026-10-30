package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.time.Instant;

// Estado de la publicación de la cátedra: hasta qué versión llegó y desde cuál conserva el detalle de cambios
public record SyncMetadata(long currentVersion, long oldestAvailableVersion, Instant publishedAt) {
}
