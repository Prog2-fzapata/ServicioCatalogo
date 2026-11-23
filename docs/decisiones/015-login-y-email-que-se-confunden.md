# 015. Impedir que el login de un usuario sea el email de otro

**Estado:** aceptada

## Contexto

El patrón de login de JHipster admite logins con forma de email, así que un mismo texto podía ser el login de un usuario y el email de otro. Alguien podía registrar como login el email de otra persona y bloquearle el ingreso por email; y si elegía la misma contraseña que ella usa, hacerla entrar a su cuenta sin que lo note.

## Decisión

Dos niveles.

- **Prevención (al escribir):** el registro y `POST /api/account` rechazan un login igual al email de otro usuario y un email igual al login de otro (`LOGIN_ALREADY_USED` y `EMAIL_ALREADY_USED`, los mismos códigos de siempre). El propio usuario sí puede usar el mismo texto como login y como email.
- **Respaldo (al ingresar):** si de todos modos hubiera dos cuentas para el mismo texto (dos registros simultáneos, datos cargados por otra vía), se prueba la contraseña contra las dos y entra la que la acierte; si acertaran ambas, gana el login. Un mismo usuario hallado por login y por email se verifica una sola vez, porque el hash es costoso.

## Alternativas descartadas

- Resolverlo solo al ingresar, sin prevenir al escribir: deja abierto el caso de la cuenta equivocada.
- Prohibir `@` en el login: es lo más simple, pero se aparta del patrón de login de JHipster.

## Consecuencias

La base no puede garantizar con una restricción única que un valor no se repita entre dos columnas distintas, así que dos registros opuestos en el mismo instante podrían colarse; para ese caso queda el respaldo del ingreso.
