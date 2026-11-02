package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis.dto;

import java.util.List;

// Forma del JSON de catedra:sync:changes:{version}. Los arrays traen ids, no entidades (anexo §14.5)
public record CatedraChangesResponse(Long version, Changes changes) {

    public record Changes(List<Long> professionalCategories, List<Long> professionals, List<Long> weeklySchedules) {
    }
}
