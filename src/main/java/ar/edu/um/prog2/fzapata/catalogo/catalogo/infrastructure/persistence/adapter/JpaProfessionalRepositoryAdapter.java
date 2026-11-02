package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PageQuery;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.PagedResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalFilter;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalSortOrder;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.specification.ProfessionalSpecifications;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaProfessionalRepositoryAdapter implements ProfessionalRepository {

    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final CatalogMapper catalogMapper;

    // Cada criterio presente se suma con AND; sin criterios, la especificación vacía devuelve todos
    private Specification<ProfessionalEntity> toSpecification(ProfessionalFilter filter) {
        List<Specification<ProfessionalEntity>> criteria = new ArrayList<>();
        if (filter.categoryId() != null) {
            criteria.add(ProfessionalSpecifications.inCategory(filter.categoryId()));
        }
        if (filter.name() != null) {
            criteria.add(ProfessionalSpecifications.nameContains(filter.name()));
        }
        if (filter.enabled() != null) {
            criteria.add(ProfessionalSpecifications.hasEnabled(filter.enabled()));
        }
        if (filter.available() != null) {
            criteria.add(ProfessionalSpecifications.hasEnabledSchedule(filter.available()));
        }
        return Specification.allOf(criteria);
    }

    private String attributeOf(ProfessionalSortOrder.Field field) {
        return switch (field) {
            case ID -> "id";
            case FIRST_NAME -> "firstName";
            case LAST_NAME -> "lastName";
        };
    }

    // El id desempata siempre: sin un orden total, dos pedidos de páginas seguidas podrían repetir o saltear filas
    Sort toSort(List<ProfessionalSortOrder> order) {
        List<Sort.Order> orders = new ArrayList<>(order.stream()
                .map(item -> new Sort.Order(
                        item.direction() == ProfessionalSortOrder.Direction.ASC ? Sort.Direction.ASC
                                : Sort.Direction.DESC,
                        attributeOf(item.field())))
                .toList());
        if (order.stream().noneMatch(item -> item.field() == ProfessionalSortOrder.Field.ID)) {
            orders.add(Sort.Order.asc("id"));
        }
        return Sort.by(orders);
    }

    @Override
    public PagedResult<Professional> search(ProfessionalFilter filter, PageQuery page,
            List<ProfessionalSortOrder> sort) {
        Page<ProfessionalEntity> found = jpaProfessionalRepository.findAll(toSpecification(filter),
                PageRequest.of(page.page(), page.size(), toSort(sort)));
        return new PagedResult<>(found.getContent().stream().map(catalogMapper::toDomainModel).toList(),
                found.getTotalElements(), page);
    }

    @Override
    public Optional<Professional> findById(Long id) {
        return jpaProfessionalRepository.findById(id).map(catalogMapper::toDomainModel);
    }
}
