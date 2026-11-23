# 012. Firmar el JWT con HS512 y un secreto compartido

**Estado:** aceptada

## Contexto

El servicio emite el JWT de usuario y turnos lo valida; ambos deben coincidir en la clave sin que la clave viaje por la red ni se versione.

## Decisión

Firma HMAC con **HS512**. La clave es una variable de entorno (`JWT_SECRET`, Base64, al menos 512 bits, generada con `openssl rand -base64 64`) y turnos usa el mismo valor. Si la clave falta, no es Base64 o es corta, la aplicación no arranca. El decoder solo acepta HS512. Librería: `oauth2-resource-server` de Spring Security (Nimbus), la misma que usa JHipster.

## Alternativas descartadas

- HS256: alcanza en seguridad, pero se aparta del algoritmo por defecto de JHipster (se había elegido por error creyendo lo contrario; se corrigió antes de emitir ningún token).
- RSA con par de claves: turnos solo tendría la clave pública y no podría fabricar tokens, pero hay que generar y distribuir archivos PEM; con dos servicios propios en el mismo entorno ese riesgo es mínimo.
- Una clave con valor por defecto en la configuración: un despliegue olvidado quedaría firmando con una clave pública. Por eso no hay valor por defecto.

## Consecuencias

Quien conozca la clave puede fabricar tokens, así que los dos `.env` deben cuidarse igual. Hay tests que rechazan tokens con otra clave, con HS256 y la clave correcta, vencidos, sin firma y con el cuerpo alterado.
