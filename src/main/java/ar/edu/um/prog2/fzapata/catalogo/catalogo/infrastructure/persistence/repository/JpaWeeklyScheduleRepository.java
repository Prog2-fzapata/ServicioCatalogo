package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.WeeklyScheduleEntity;

@Repository
public interface JpaWeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, Long> {
}
