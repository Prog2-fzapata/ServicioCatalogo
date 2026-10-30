package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategoryFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CategorySortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalCategoryRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaProfessionalCategoryRepositoryAdapter implements ProfessionalCategoryRepository {

    private final JpaProfessionalCategoryRepository jpaProfessionalCategoryRepository;
    private final CatalogMapper catalogMapper;

    // Sin criterio, la especificación vacía devuelve todas
    private Specification<ProfessionalCategoryEntity> toSpecification(CategoryFilter filter) {
        List<Specification<ProfessionalCategoryEntity>> criteria = new ArrayList<>();
        if (filter.enabled() != null) {
            criteria.add((root, query, builder) -> builder.equal(root.get("enabled"), filter.enabled()));
        }
        return Specification.allOf(criteria);
    }

    private String attributeOf(CategorySortOrder.Field field) {
        return switch (field) {
            case ID -> "id";
            case NAME -> "name";
        };
    }

    // El id desempata siempre: sin un orden total, dos pedidos de páginas seguidas podrían repetir o saltear filas
    Sort toSort(List<CategorySortOrder> order) {
        List<Sort.Order> orders = new ArrayList<>(order.stream()
                .map(item -> new Sort.Order(
                        item.direction() == CategorySortOrder.Direction.ASC ? Sort.Direction.ASC
                                : Sort.Direction.DESC,
                        attributeOf(item.field())))
                .toList());
        if (order.stream().noneMatch(item -> item.field() == CategorySortOrder.Field.ID)) {
            orders.add(Sort.Order.asc("id"));
        }
        return Sort.by(orders);
    }

    @Override
    public List<ProfessionalCategory> findAllById(Collection<Long> ids) {
        return jpaProfessionalCategoryRepository.findAllById(ids).stream()
                .map(catalogMapper::toDomainModel)
                .toList();
    }

    @Override
    public Optional<ProfessionalCategory> findById(Long id) {
        return jpaProfessionalCategoryRepository.findById(id).map(catalogMapper::toDomainModel);
    }

    @Override
    public PagedResult<ProfessionalCategory> search(CategoryFilter filter, PageQuery page,
            List<CategorySortOrder> sort) {
        Page<ProfessionalCategoryEntity> found = jpaProfessionalCategoryRepository.findAll(toSpecification(filter),
                PageRequest.of(page.page(), page.size(), toSort(sort)));
        return new PagedResult<>(found.getContent().stream().map(catalogMapper::toDomainModel).toList(),
                found.getTotalElements(), page);
    }
}
