# 032. Actualizar la cuenta solo con los campos que llegan

**Estado:** aceptada

## Contexto

`POST /api/account` reemplazaba todos los campos editables y exigía el email, aunque el usuario se identifica por el claim `uid` del token y no por el email. Quien se registra con login, email y contraseña, sin nombre, y necesita cargarlo (turnos lo exige para reservar: `PATIENT_NAME_REQUIRED`), tenía que reenviar su email, y si omitía el apellido, se borraba sin avisar.

## Decisión

La actualización es parcial. Ningún campo es obligatorio: uno ausente (o en `null`) conserva su valor, y un cuerpo vacío es válido y no cambia nada. Para borrar `firstName`, `lastName` o `imageUrl` se envía un texto vacío, y el dato queda sin valor. El email, si llega, mantiene su formato y largo y la comprobación de que no sea el email ni el login de otro usuario, que ahora solo se hace cuando llega. `langKey` ya conservaba el valor cuando faltaba.

**Justificación:** cambiar un dato no debería obligar a leer la cuenta y reenviarla entera, ni borrar lo que no se mencionó.

## Alternativas descartadas

- Mantener el reemplazo completo de JHipster: obliga a reenviar todos los campos para cambiar uno y un campo omitido borra datos en silencio.
- Pasar a `PATCH`: es lo más correcto, pero la ruta y el verbo forman parte del contrato compatible con JHipster que pide el enunciado ([decisión 007](007-usuarios-compatibles-con-jhipster.md)).
- Distinguir un `null` explícito de un campo ausente para poder borrar: exige una deserialización especial (`JsonNullable` o un mapa); con el texto vacío alcanza.

## Consecuencias

Un cliente que envía el cuerpo completo, como en JHipster, obtiene el mismo resultado. Cambia solo lo omitido: antes se borraba y ahora se conserva, así que quien dependiera de omitir para borrar debe enviar un texto vacío.
