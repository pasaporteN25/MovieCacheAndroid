# Hoja de ruta — movieIndexAndroid

`tareas.md` es la fuente de verdad de alcance, dependencias y estado. Esta hoja guarda el
orden, el porqué y las decisiones que no conviene reabrir sin querer.

## El norte

Una aplicación que **funciona sola** y **se conecta cuando vos querés**:

- Se aparea **una vez** con una cuenta que ya existe en tu instancia de Movie Inbox,
  escaneando un QR de la web.
- Desde ahí tiene **su propia copia** de tu catálogo y funciona sin conexión: explorar,
  buscar, editar tu estado personal y dar de alta obras.
- La **sincronización la inicia una persona** y es bidireccional. Un borrado viaja al otro lado
  sólo como registro de que una persona borró: que una obra falte **nunca borra nada**. Si los
  dos lados cambiaron lo mismo de forma distinta, el puntaje se resuelve solo por el más alto,
  y en lo demás **decide la persona**.

"Independiente" no quiere decir "sin acuerdo con el servidor": cuando sincronizan, los dos
tienen que hablar el mismo idioma. Ese idioma es el contrato copiado en `contract/`.

## Hitos

| Hito | Qué podés hacer con el teléfono | Tareas | Del lado del servidor |
| --- | --- | --- | --- |
| **M0 — Entorno** | Compilar el proyecto | [A2.0], cerrada 2026-09-13 | — |
| **M1 — Sincronización probada** | Todavía nada en la mano: saber que la fusión es correcta en cada caso antes de construir pantallas | [A5] | [X2], que la matriz confirmó; [X4], sesiones que no se pierden por un corte; [X5], bajas que viajan |
| **M2 — Aparear y leer** | Aparear por QR y ver tu catálogo sin conexión | [A2.1], [A3.1] | Hecho, salvo la pantalla del QR y los vectores del pin ([X1.1]) |
| **M3 — Editar** | Marcar vistas, puntuar y escribir reviews sin red; resolver conflictos | [A2.2] | Lo que salga de [A5] |
| **M4 — Alta sin conexión** | Guardar películas en cualquier lado | [A2.3], [A3.2] | Hecho, salvo los vectores de normalización ([X1.2]) |
| **M5 — Charadas** | Jugar con el mismo mazo en varios teléfonos, sin red | [A2.4], [A3.3] | Hecho |
| **M6 — Imágenes** | Miniaturas locales y portada en segundo plano | [A2.5] | — |
| **M7 — Lo que viaja** | Colecciones seguidas, disponibilidad y puntajes | [A2.6] | Hecho |
| **M8 — Buscar bien sin conexión** | Buscar en tu réplica con un ranking medido | [A3.4] | El corpus dorado ya existe |

El orden sigue al caso de uso que manda —guardar películas sin estar en casa— y es el que
fijó el owner para [A2], con un cambio del 2026-09-13: **la sincronización se prueba
primero**, porque es lo que puede obligar a cambiar el contrato con el servidor. La búsqueda
sin conexión va al final como ranking medido, pero desde M2 hay un filtro simple por título.

**v0.1.0** se publica al cerrar M2: es la primera versión que sirve para algo en la mano.

## Decisiones que rigen

Las fuentes están en el repositorio del servidor, salvo las marcadas como de este repo.

| Fecha | Decisión | Fuente |
| --- | --- | --- |
| 2026-08-17 | El cliente es un proyecto y un repositorio aparte | `docs/briefs/plan-inicial-2026-08-17.md` (este repo) |
| 2026-09-02 | API de dispositivo bajo `/api/v1/`: tokens opacos por dispositivo en `Authorization`, sin cookies ni CORS; Scanner, administración y Curaduría quedan afuera | ADR-0003 y contrato congelado en `34c2c24` |
| 2026-09-07 | Cliente autónomo con almacén propio; sincronización que inicia una persona y nunca borra; fusión a tres bandas por campo | ADR-0005 |
| 2026-09-07 | Kotlin nativo; borradores sin conexión que no vencen; miniatura local más portada en segundo plano; una cuenta por instalación | ADR-0005 |
| 2026-09-07 | **Cuenta obligatoria**: la app requiere una cuenta creada en la web, así que aparear es lo primero | Enmienda de ADR-0005; `docs/briefs/android-client-v3.md` (este repo) |
| 2026-09-07 | `minSdk` 26, Room, WorkManager, ZXing embebido y ninguna biblioteca de sincronización de terceros | `docs/briefs/android-client-v3.md` (este repo) |
| 2026-09-09 | En la red de casa, HTTPS en el propio servidor con certificado autofirmado; la huella SPKI viaja en el QR | `docs/deployment.md`, "HTTPS en la red local, sin dominio" |
| 2026-09-12 | Las claves de charadas viajan tal como las calcula el servidor, no como ids opacos | `docs/briefs/charades-v1.md` |
| 2026-09-13 | `compileSdk` 36 hasta instalar la plataforma 37 | `gradle/libs.versions.toml` (este repo) |
| 2026-09-13 | Al desaparear, los datos del teléfono persisten | Owner; el detalle, en [A5.1] |
| 2026-09-13 | La sincronización se prueba antes de construir pantallas | Owner; [A5] |
| 2026-09-13 | Licencia GPL-3.0; remoto `pasaporteN25/MovieCacheAndroid` | Owner |
| 2026-09-14 | Con el teléfono desapareado se puede seguir editando; los cambios viajan al volver a aparear la misma cuenta | Owner; matriz de [A5.1] |
| 2026-09-14 | Para cambiar de cuenta, primero se sincroniza la actual | Owner; matriz de [A5.1] |
| 2026-09-14 | El servidor registra cuándo cambia cada campo personal, en el backlog | Owner; [X3] del servidor |
| 2026-09-14 | Un conflicto de puntaje se resuelve solo, por el más alto. Es así por definición y se puede cambiar | Owner; matriz de [A5.1] |
| 2026-09-14 | La base de un campo sólo avanza con un valor que confirmó el servidor. Si trae problemas, la alternativa es descartar la sincronización que quedó a medias y repetirla | Owner; invariante 3 de `CLAUDE.md` |
| 2026-09-14 | Los borrados viajan, sólo como registro de que una persona borró; borrar de un lado y editar del otro lo decide la persona; unir duplicados mueve lo pendiente a la obra que queda | Owner; revierte "nunca borra" de ADR-0005; [X5] del servidor |
| 2026-09-14 | La llave de un teléfono vence: pasado un mes hay que volver a escanear el QR, y podría pedirse más seguido | Owner, que revisó ese mismo día un "no vence" anterior; [X4] del servidor |

## Abierto

PIN o biometría propios, además de la pantalla de bloqueo.

Y dos preguntas que dejó [A5.1] (`docs/analisis/matriz-de-sincronizacion-2026-09-14.md`):
1. cómo se resuelve un conflicto de review o de "visto", ahora que el de puntaje se resuelve
   solo;
2. si el mes de la llave se cuenta desde la última vez que el teléfono la renovó, como hoy, o
   desde que se apareó, aunque se use.
