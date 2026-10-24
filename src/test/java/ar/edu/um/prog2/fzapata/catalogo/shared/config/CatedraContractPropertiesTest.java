package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatedraContractPropertiesTest {

    private static final Map<String, String> ALL = Map.of(
            "catedra.contract.snapshot-path", "/api/synchronization/snapshot",
            "catedra.contract.redis-keys.metadata", "catedra:sync:metadata",
            "catedra.contract.redis-keys.changes-prefix", "catedra:sync:changes:",
            "catedra.contract.redis-keys.categories", "catedra:sync:professional-categories",
            "catedra.contract.redis-keys.professionals", "catedra:sync:professionals",
            "catedra.contract.redis-keys.weekly-schedules", "catedra:sync:weekly-schedules");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CatedraContractConfiguration.class);

    private String[] valuesWithout(String name) {
        List<String> values = new ArrayList<>();
        ALL.forEach((key, value) -> {
            if (!key.equals(name)) {
                values.add(key + "=" + value);
            }
        });
        return values.toArray(String[]::new);
    }

    @Test
    void bindsEveryNameFromTheConfiguration() {
        contextRunner.withPropertyValues(valuesWithout("ninguna")).run(context -> {
            CatedraContractProperties contract = context.getBean(CatedraContractProperties.class);
            assertThat(contract).isEqualTo(CatedraContractFixtures.v1());
        });
    }

    // El formato se exige solo en la ruta: las claves de Redis pueden seguir la convención que quiera la cátedra
    @ParameterizedTest
    @ValueSource(strings = {"api/synchronization/snapshot", "/", "http://otro.host/snapshot"})
    void contextFailsWhenTheSnapshotPathDoesNotStartWithASlash(String path) {
        contextRunner.withPropertyValues(valuesWithout("catedra.contract.snapshot-path"))
                .withPropertyValues("catedra.contract.snapshot-path=" + path)
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("debe empezar con /"));
    }

    // Sin ninguna clave, Spring no arma el grupo de claves (queda null): lo detecta @NotNull, no @NotBlank
    @Test
    void contextFailsWithoutAnyRedisKey() {
        contextRunner.withPropertyValues("catedra.contract.snapshot-path=/api/synchronization/snapshot")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("redisKeys"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "catedra.contract.snapshot-path",
            "catedra.contract.redis-keys.metadata",
            "catedra.contract.redis-keys.changes-prefix",
            "catedra.contract.redis-keys.categories",
            "catedra.contract.redis-keys.professionals",
            "catedra.contract.redis-keys.weekly-schedules"})
    void contextFailsWhenAnyNameIsMissing(String missing) {
        contextRunner.withPropertyValues(valuesWithout(missing)).run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "catedra.contract.snapshot-path",
            "catedra.contract.redis-keys.metadata",
            "catedra.contract.redis-keys.changes-prefix",
            "catedra.contract.redis-keys.categories",
            "catedra.contract.redis-keys.professionals",
            "catedra.contract.redis-keys.weekly-schedules"})
    void contextFailsWhenAnyNameIsBlank(String blank) {
        contextRunner.withPropertyValues(valuesWithout(blank)).withPropertyValues(blank + "= ")
                .run(context -> assertThat(context).hasFailed());
    }
}
