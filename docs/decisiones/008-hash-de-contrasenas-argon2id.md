# 008. Guardar las contraseñas con Argon2id

**Estado:** aceptada

## Contexto

El enunciado exige almacenar las contraseñas con un mecanismo criptográfico adecuado y respetar las validaciones definidas por la cátedra, cuyo registro admite contraseñas de 4 a 100 caracteres. La primera versión usaba BCrypt precedido de un SHA-256 en Base64, porque BCrypt solo procesa 72 bytes y los caracteres como `ñ` pesan 2. BCrypt es la generación anterior de hashes de contraseñas, y OWASP recomienda Argon2id en primer lugar.

## Decisión

- `Argon2PasswordHasher` implementa el puerto `PasswordHasher` con el `Argon2PasswordEncoder` de Spring Security, que usa las clases de Argon2 de BouncyCastle. La contraseña se hashea tal cual, sin paso previo: Argon2id no limita su largo.
- Parámetros: los mínimos de OWASP, **19 MiB de memoria, 2 iteraciones y paralelismo 1**, con sal de 16 bytes y hash de 32. En la máquina de desarrollo cada hash tarda unos 32 ms (BCrypt con costo 10 tardaba unos 91 ms) y la memoria que pide Java coincide con el parámetro.
- El costo se configura por entorno: `PASSWORD_ARGON2_MEMORY_KIB`, `PASSWORD_ARGON2_ITERATIONS` y `PASSWORD_ARGON2_PARALLELISM`, definidas en `.env`, sin valor por defecto en `application.properties`. `Argon2Properties` las valida al arrancar y el servicio no arranca si falta alguna o si está por debajo de los mínimos de OWASP, para que nadie debilite el hash por error. `Argon2Configuration` (en `shared/security`, como la del JWT) arma el encoder.
- El hash guarda sus propios parámetros (`$argon2id$v=19$m=19456,t=2,p=1$...`): cambiar el costo más adelante no invalida los hashes ya guardados, que se verifican con el costo con el que se crearon.
- La migración `V3` ensancha `users.password_hash` de `VARCHAR(60)` a `VARCHAR(255)`, porque un hash Argon2id mide 97 caracteres. La `V1` ya estaba aplicada y no se edita.
- Los casos de uso no cambian: el dominio solo conoce `PasswordHasher`.

## Alternativas descartadas

- Seguir con BCrypt y SHA-256: BCrypt es la generación anterior (OWASP lo deja para sistemas heredados) y necesita ese paso previo por su límite de 72 bytes.
- Los valores por defecto de Spring (16 MiB): quedan algo por debajo del mínimo de OWASP.
- Costos fijos en el código: cambiarlos exigiría recompilar.
- Un hasher que reconozca BCrypt y Argon2id y vuelva a hashear al iniciar sesión: solo serviría con usuarios ya registrados, y todavía no hay.
- `argon2-jvm`: es una biblioteca nativa, más difícil de desplegar.

## Consecuencias

Cada registro o login ocupa unos 19 MiB durante unos 30 ms, así que 50 simultáneos serían alrededor de 1 GB de memoria temporal, y BouncyCastle suma unos 7 MB a la imagen. Un hash de otro formato (por ejemplo, de una base anterior con BCrypt) no se verifica: `matches` devuelve `false` sin lanzar excepción y el login responde como credenciales inválidas; un test lo comprueba. En una base de desarrollo con usuarios viejos hay que reiniciarla o registrarse de nuevo. El rango de 4 a 100 caracteres y el máximo de 100 en el login se mantienen: este último evita hashear entradas enormes con un algoritmo costoso.
