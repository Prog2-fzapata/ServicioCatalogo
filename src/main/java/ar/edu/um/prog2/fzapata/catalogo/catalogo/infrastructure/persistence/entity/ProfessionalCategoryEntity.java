package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "professional_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalCategoryEntity {

    // Id de la cátedra: no se genera acá
    @Id
    private Long id;

    private String name;

    private String description;

    private boolean enabled;

    private Instant createdAt;

    private Instant updatedAt;
}
