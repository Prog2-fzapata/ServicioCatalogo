package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.ProfessionalEntity;

@Repository
public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, Long> {
}
