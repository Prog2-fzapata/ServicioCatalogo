package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraContractProperties.RedisKeys;

// Nombres del contrato para los tests: los del contrato v1 y una variante «renombrada» para probar que no están fijos
public final class CatedraContractFixtures {

    private CatedraContractFixtures() {
    }

    public static CatedraContractProperties v1() {
        return new CatedraContractProperties("/api/synchronization/snapshot", new RedisKeys(
                "catedra:sync:metadata", "catedra:sync:changes:", "catedra:sync:professional-categories",
                "catedra:sync:professionals", "catedra:sync:weekly-schedules"), "catedra.catalog.proyecto-de-prueba");
    }

    public static CatedraContractProperties renamed() {
        return new CatedraContractProperties("/api/v2/sync/snapshot", new RedisKeys(
                "nueva:meta", "nueva:cambios:", "nueva:categorias", "nueva:profesionales", "nueva:horarios"),
                "nuevo.topic.catalogo");
    }
}
