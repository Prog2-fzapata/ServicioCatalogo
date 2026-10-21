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
@Table(name = "professionals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalEntity {

    @Id
    private Long id;

    // Solo el id, sin @ManyToOne: la clave foránea la garantiza la base y se evitan cargas perezosas
    private Long categoryId;

    private String firstName;

    private String lastName;

    private boolean enabled;

    private Instant createdAt;

    private Instant updatedAt;
}
