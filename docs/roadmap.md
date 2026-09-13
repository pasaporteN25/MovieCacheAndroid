# Hoja de ruta — movieIndexAndroid

`tareas.md` es la fuente de verdad de alcance, dependencias y estado. Esta hoja guarda el
orden, el porqué y las decisiones que no conviene reabrir sin querer.

## El norte

Una aplicación que **funciona sola** y **se conecta cuando vos querés**:

- Se aparea **una vez** con una cuenta que ya existe en tu instancia de Movie Inbox,
  escaneando un QR de la web.
- Desde ahí tiene **su propia copia** de tu catálogo y funciona sin conexión: explorar,
  buscar, editar tu estado personal y dar de alta obras.
- La **sincronización la inicia una persona**, es bidireccional y **nunca borra**. Si los dos
  lados cambiaron lo mismo de forma distinta, **decide la persona**.

"Independiente" no quiere decir "sin acuerdo con el servidor": cuando sincronizan, los dos
tienen que hablar el mismo idioma. Ese idioma es el contrato copiado en `contract/`.

## Hitos

| Hito | Qué podés hacer con el teléfono | Tareas | Del lado del servidor |
| --- | --- | --- | --- |
| **M0 — Entorno** | Compilar el proyecto | [A2.0], cerrada 2026-09-13 | — |
| **M1 — Aparear y leer** | Aparear por QR y ver tu catálogo sin conexión | [A2.1], [A3.1] | Hecho, salvo la pantalla del QR y los vectores del pin ([X1.1]) |
| **M2 — Editar** | Marcar vistas, puntuar y escribir reviews sin red; resolver conflictos | [A2.2] | Hecho |
| **M3 — Alta sin conexión** | Guardar películas en cualquier lado | [A2.3], [A3.2] | Hecho, salvo los vectores de normalización ([X1.2]) |
| **M4 — Charadas** | Jugar con el mismo mazo en varios teléfonos, sin red | [A2.4], [A3.3] | Hecho |
| **M5 — Imágenes** | Miniaturas locales y portada en segundo plano | [A2.5] | — |
| **M6 — Lo que viaja** | Colecciones seguidas, disponibilidad y puntajes | [A2.6] | Hecho |
| **M7 — Buscar bien sin conexión** | Buscar en tu réplica con un ranking medido | [A3.4] | El corpus dorado ya existe |

El orden sigue al caso de uso que manda —guardar películas sin estar en casa— y es el que
fijó el owner para [A2]. La búsqueda sin conexión va al final como ranking medido, pero
desde M1 hay un filtro simple por título.

**v0.1.0** se publica al cerrar M1: es la primera versión que sirve para algo en la mano.

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

## Abierto

Las cuatro decisiones del owner que lista `tareas.md`: qué pasa con los datos al desaparear,
PIN o biometría propios, licencia y repositorio remoto.
