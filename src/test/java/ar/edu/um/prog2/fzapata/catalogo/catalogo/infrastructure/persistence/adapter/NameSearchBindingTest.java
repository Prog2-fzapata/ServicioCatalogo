package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import lombok.RequiredArgsConstructor;

// Contra PostgreSQL real: el texto que escribe el usuario debe viajar como parámetro, no escrito dentro del SQL
@DataJpaTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter."
        + "NameSearchBindingTest$SqlRecorder")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaProfessionalRepositoryAdapter.class,
        JpaProfessionalCategoryRepositoryAdapter.class, JpaWeeklyScheduleRepositoryAdapter.class, CatalogMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class NameSearchBindingTest {

    // Guarda cada sentencia que Hibernate manda a la base, con un ? en lugar de cada parámetro
    public static class SqlRecorder implements StatementInspector {

        static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

        @Override
        public String inspect(String sql) {
            STATEMENTS.add(sql);
            return sql;
        }
    }

    private final JpaProfessionalRepositoryAdapter adapter;

    @Test
    void theSearchedTextTravelsAsAParameterAndNotInsideTheSql() {
        SqlRecorder.STATEMENTS.clear();

        adapter.search(new ProfessionalFilter(null, "o'brien' OR 1=1 --", null, null), new PageQuery(0, 20),
                ProfessionalSortOrder.byName());

        assertThat(SqlRecorder.STATEMENTS).isNotEmpty().noneMatch(sql -> sql.contains("brien") || sql.contains("1=1"));
    }
}
