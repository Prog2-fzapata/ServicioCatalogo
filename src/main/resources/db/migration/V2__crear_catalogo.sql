-- Copia local del catálogo de la cátedra. Los ids son los de la cátedra (no se generan acá) para poder
-- aplicar cambios por id; las bajas son lógicas (enabled = false), nunca se borran filas.
CREATE TABLE professional_categories (
    id          BIGINT       PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    enabled     BOOLEAN      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE professionals (
    id          BIGINT       PRIMARY KEY,
    category_id BIGINT       NOT NULL,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    enabled     BOOLEAN      NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_professionals_category FOREIGN KEY (category_id) REFERENCES professional_categories (id)
);

-- PostgreSQL no indexa las claves foráneas por su cuenta
CREATE INDEX idx_professionals_category_id ON professionals (category_id);

CREATE TABLE weekly_schedules (
    id                    BIGINT      PRIMARY KEY,
    professional_id       BIGINT      NOT NULL,
    day_of_week           VARCHAR(9)  NOT NULL,
    start_time            TIME        NOT NULL,
    end_time              TIME        NOT NULL,
    slot_duration_minutes INTEGER     NOT NULL,
    enabled               BOOLEAN     NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_weekly_schedules_professional FOREIGN KEY (professional_id) REFERENCES professionals (id),
    CONSTRAINT ck_weekly_schedules_day CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_weekly_schedules_time_range CHECK (end_time > start_time),
    CONSTRAINT ck_weekly_schedules_slot_duration CHECK (slot_duration_minutes > 0)
);

CREATE INDEX idx_weekly_schedules_professional_id ON weekly_schedules (professional_id);

-- Una sola fila (id = 1), creada acá: quien lee el estado siempre la encuentra, y la versión 0
-- significa "base vacía" (hace falta un snapshot). local_version solo cambia en la misma
-- transacción que aplica los datos.
CREATE TABLE sync_state (
    id             INTEGER     PRIMARY KEY,
    local_version  BIGINT      NOT NULL DEFAULT 0,
    last_sync_at   TIMESTAMPTZ,
    last_sync_type VARCHAR(11),
    last_status    VARCHAR(5),
    last_error     TEXT,
    CONSTRAINT ck_sync_state_single_row CHECK (id = 1),
    CONSTRAINT ck_sync_state_type CHECK (last_sync_type IN ('FULL', 'INCREMENTAL')),
    CONSTRAINT ck_sync_state_status CHECK (last_status IN ('OK', 'ERROR'))
);

INSERT INTO sync_state (id, local_version) VALUES (1, 0);

-- Deduplicación de eventos de Kafka (entrega al menos una vez): la clave primaria rechaza el repetido
CREATE TABLE processed_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
