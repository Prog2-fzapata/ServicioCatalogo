package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// Qué hacer para llevar la copia local a la versión de la cátedra
public sealed interface SyncPlan {

    // La copia ya tiene la versión actual
    record UpToDate() implements SyncPlan {
    }

    // Reemplazar todo con un snapshot; reason explica por qué no alcanza la incremental
    record Snapshot(String reason) implements SyncPlan {
    }

    // Aplicar de a una las versiones from + 1 hasta to, ambas inclusive
    record Incremental(long from, long to) implements SyncPlan {
    }
}
