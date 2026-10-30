package ar.edu.um.prog2.fzapata.catalogo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogoApplicationTests {

	@Test
	void contextLoads() {
	}

}
