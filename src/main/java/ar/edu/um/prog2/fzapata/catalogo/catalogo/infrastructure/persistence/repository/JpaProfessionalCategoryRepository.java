package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalCategoryEntity;

@Repository
public interface JpaProfessionalCategoryRepository extends JpaRepository<ProfessionalCategoryEntity, Long>, JpaSpecificationExecutor<ProfessionalCategoryEntity> {
}
