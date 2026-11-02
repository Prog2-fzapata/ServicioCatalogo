package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity;

import java.time.Instant;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sync_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStateEntity {

    // La tabla tiene una sola fila, creada por la migración V2
    public static final int SINGLE_ROW_ID = 1;

    @Id
    private Integer id;

    private long localVersion;

    private Instant lastSyncAt;

    @Enumerated(EnumType.STRING)
    private SyncType lastSyncType;

    @Enumerated(EnumType.STRING)
    private SyncStatus lastStatus;

    private String lastError;
}
