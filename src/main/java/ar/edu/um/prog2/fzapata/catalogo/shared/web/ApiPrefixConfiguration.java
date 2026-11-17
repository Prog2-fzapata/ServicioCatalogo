package ar.edu.um.prog2.fzapata.catalogo.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ar.edu.um.prog2.fzapata.catalogo.CatalogoApplication;

// Prefijo común de las rutas de la API: se escribe acá y no en cada controller
@Configuration
public class ApiPrefixConfiguration implements WebMvcConfigurer {

    public static final String API_PREFIX = "/api";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Solo los controllers propios, por paquete: los de springdoc (/v3/api-docs) también son @RestController y el
        // predicado acepta un tipo apenas cumple uno de sus criterios, así que no se le suma la anotación
        configurer.addPathPrefix(API_PREFIX, HandlerTypePredicate.forBasePackage(
                CatalogoApplication.class.getPackageName()));
    }
}
