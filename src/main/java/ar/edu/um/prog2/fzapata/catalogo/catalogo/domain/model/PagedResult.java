package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.util.List;
import java.util.function.Function;

// Una página de resultados y el total de elementos que cumplen el criterio, no solo los de esta página
public record PagedResult<T>(List<T> content, long totalElements, PageQuery pageQuery) {

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / pageQuery.size());
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), totalElements, pageQuery);
    }
}
