package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Direction;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder.Field;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.exception.InvalidSearchParameterException;

// Traduce los parámetros del pedido a los tipos del dominio, que no conocen a Spring
@Component
public class ProfessionalQueryMapper {

    public static final int MAX_NAME_LENGTH = 100;
    private static final String SEARCH_OBJECT = "professionalSearch";

    // Los únicos nombres de campo que acepta sort, como aparecen en la respuesta
    private static final Map<String, Field> SORTABLE = Map.of(
            "id", Field.ID,
            "firstName", Field.FIRST_NAME,
            "lastName", Field.LAST_NAME);

    public ProfessionalFilter toFilter(Long categoryId, String name, Boolean enabled, Boolean available) {
        // Al ser name opcional, puede ser null. En caso de no serlo, no puede ser superior al valor máximo
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new InvalidSearchParameterException(SEARCH_OBJECT, "name",
                    "El nombre no puede tener más de " + MAX_NAME_LENGTH + " caracteres");
        }
        return new ProfessionalFilter(categoryId, name, enabled, available);
    }

    public PageQuery toPageQuery(Pageable pageable) {
        return new PageQuery(pageable.getPageNumber(), pageable.getPageSize());
    }

    // Ordenados porque Map.of no garantiza el orden de sus claves
    private static String allowedFields() {
        return String.join(", ", new TreeSet<>(SORTABLE.keySet()));
    }

    // Sin sort en el pedido, apellido y nombre
    public List<ProfessionalSortOrder> toSortOrder(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return ProfessionalSortOrder.byName();
        }
        List<ProfessionalSortOrder> order = new ArrayList<>();
        // Pageable trae un Sort que es una colección de Sort.Order, cada uno trae su property y direction
        for (Sort.Order item : pageable.getSort()) {
            Field field = SORTABLE.get(item.getProperty());
            if (field == null) {
                throw new InvalidSearchParameterException(SEARCH_OBJECT, "sort", "No se puede ordenar por '"
                        + item.getProperty() + "'; los campos permitidos son: " + allowedFields());
            }
            order.add(new ProfessionalSortOrder(field, item.isAscending() ? Direction.ASC : Direction.DESC));
        }
        return order;
    }
}
