CREATE TABLE users (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login              VARCHAR(50)  NOT NULL,
    password_hash      VARCHAR(60)  NOT NULL,
    first_name         VARCHAR(50),
    last_name          VARCHAR(50),
    email              VARCHAR(254) NOT NULL,
    image_url          VARCHAR(256),
    lang_key           VARCHAR(10)  NOT NULL DEFAULT 'es',
    activated          BOOLEAN      NOT NULL,
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_modified_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_login UNIQUE (login),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE authorities (
    name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE user_authorities (
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    authority_name VARCHAR(50) NOT NULL REFERENCES authorities (name),
    PRIMARY KEY (user_id, authority_name)
);

INSERT INTO authorities (name) VALUES ('ROLE_USER');
