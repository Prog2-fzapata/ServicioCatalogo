-- Un hash Argon2id mide unos 97 caracteres; los 60 de BCrypt ya no alcanzan
ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(255);
