package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;

// Ids afectados por una versión. Las entidades completas se leen aparte, con su estado vigente
public record CatalogChanges(long version, List<Long> categoryIds, List<Long> professionalIds,
        List<Long> weeklyScheduleIds) {
}
