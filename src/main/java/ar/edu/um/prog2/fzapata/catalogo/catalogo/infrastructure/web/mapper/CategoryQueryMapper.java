package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.exception.InvalidSearchParameterException;

// Traduce los parámetros del pedido a los tipos del dominio, que no conocen a Spring
@Component
public class CategoryQueryMapper {

    // objectName para el primer argumento de las excepciones
    private static final String SEARCH_OBJECT = "categorySearch";

    // Los únicos nombres de campo que acepta sort, como aparecen en la respuesta
    private static final Map<String, Field> SORTABLE = Map.of(
            "id", Field.ID,
            "name", Field.NAME);

    // True filtra enabled o false filtra disabled. Si es null, no filtra.
    public CategoryFilter toFilter(Boolean enabled) {
        return new CategoryFilter(enabled);
    }

    // Convierte a PageQuery con número de página y tamaño.
    public PageQuery toPageQuery(Pageable pageable) {
        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
    }

    // Ordenados porque Map.of no garantiza el orden de sus claves
    private static String allowedFields() {
        return String.join(", ", new TreeSet<>(SORTABLE.keySet()));
    }

    // Sin sort en el pedido, por nombre
    public List<CategorySortOrder> toSortOrder(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return CategorySortOrder.byName();
        }
        List<CategorySortOrder> order = new ArrayList<>();
        for (Sort.Order item : pageable.getSort()) {
            Field field = SORTABLE.get(item.getProperty());
            if (field == null) {
                throw new InvalidSearchParameterException(SEARCH_OBJECT, "sort", "No se puede ordenar por '"
                        + item.getProperty() + "'; los campos permitidos son: " + allowedFields());
            }
            order.add(new CategorySortOrder(field, item.isAscending() ? Direction.ASC : Direction.DESC));
        }
        return order;
    }
}
