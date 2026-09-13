# Tareas — movieIndexAndroid

Tablero en Markdown, versionado en el repo, con las mismas reglas que el del servidor.
Columnas = estado (`Backlog` / `En curso` / `Hecho`). Las tareas se agrupan por frente, con
alcance concreto, dependencias explícitas y un nivel de modelo sugerido: mecánica de un solo
archivo sin decisiones → chico; pocas piezas con las decisiones ya tomadas → medio; requiere
criterio, coordina varias piezas o toca identidad, sincronización o seguridad → grande. Al
cerrar una tarea: moverla a `Hecho` con fecha y commit, no borrarla.

**Origen.** Este tablero arrancó el 2026-09-13 con la épica [A2], que hasta ese día vivía en
el `tareas.md` del servidor. Los números [A2.x] se conservan porque los commits del servidor
los citan; las épicas nuevas de este repo siguen la serie: [A3], [A4]. Lo que el servidor
tiene que hacer para este cliente vive en el tablero del servidor, y acá se cita con su
número de allá, como [X1.1].

---

## Backlog

## Resumen operativo

| Orden | Tarea | Resultado esperado | Depende de |
| --- | --- | --- | --- |
| 1 | [A2.1] | Aparear y ver el catálogo real sin conexión | [A3.1]; decisión de desapareo (owner) |
| 2 | [A2.2] | Editar el estado personal sin conexión, con fusión a tres bandas | [A2.1] |
| 3 | [A2.3] | Dar de alta obras sin conexión | [A2.1]; [A3.2] |
| 4 | [A2.4] | Charadas sin conexión, con el mismo mazo que el servidor | [A2.1]; [A3.3] |
| 5 | [A2.5] | Imágenes: miniatura local y portada en segundo plano | [A2.1] |
| 6 | [A2.6] | Colecciones seguidas, disponibilidad y puntajes públicos | [A2.1] |
| 7 | [A3.4] | Búsqueda sin conexión con el ranking medido contra el corpus del servidor | [A2.1] |
| — | [A4] | Calidad y entrega: CI, prueba del contrato, `compileSdk` 37, firma | se toma cuando haga falta |

**Para probar [A2.1] de punta a punta** hacen falta dos cosas que no son de este repo: la
pantalla del servidor que genera el QR, que es un traspaso al frente visual de allá, y el
certificado de la red local, que emite el owner con la receta de `docs/deployment.md` del
servidor.

### Frente: Cliente Android

#### [A2] Cliente Android autónomo

- **Alcance**: aplicación Android nativa (Kotlin + Compose) con almacén local propio. Se
  aparea una vez con una cuenta que ya existe en la instancia, y **desde ahí funciona sin
  conexión**: explorar, buscar, editar estado personal y **dar de alta obras nuevas**. La
  sincronización es bidireccional, la inicia una persona y **nunca borra**.
- **Caso de uso que manda**, dicho por el owner: *guardar películas en la colección sin estar
  frente a la computadora ni en casa*. Todo el orden de entrega sale de ahí.
- **Criterio de cierre**: el teléfono muestra y edita el catálogo real sin conexión, y una
  obra dada de alta sin red llega al servidor por el camino de importación y revisión que ya
  existe, sin decidir identidad por su cuenta.
- **Plan vigente**: `docs/briefs/android-client-v3.md`. Dirección: ADR-0005 del servidor,
  enmendada por la decisión de la cuenta obligatoria.
- **Modelo sugerido**: Grande. Es un proyecto cliente completo, no una pantalla más.

**La distinción que sostiene el diseño**: editar una obra que ya existe de los dos lados
tiene **base compartida** y se resuelve por fusión a tres bandas; dar de alta una obra que no
existe en ningún lado **no tiene base**, así que no es una fusión sino una **importación**, y
va por el camino de revisión que ya existe. Confundirlas obligaría a inventar una base.

  - [ ] **[A2.1] Esqueleto, apareamiento y lectura sin conexión.** El primer hito pedido por
    el owner: **aparear y ver el catálogo real en el teléfono**. Hilt con KSP, coroutines y
    `StateFlow`; el repositorio es el límite de errores. Room con la forma del contrato
    portable v9 —capa de obra y capa personal, **nunca** la operativa—. Escaneo del QR con
    ZXing embebido, para no arrastrar Play Services. Descarga completa, paginada por cursor.
    **Servidor: hecho** —ticket de un solo uso, canje en `POST /api/v1/pair`, QR dibujado en
    el servidor y pin derivado del certificado; commits `334f4fe`, `e9bcaab` y `11dbe0b`—,
    salvo la pantalla que muestra el QR.
    **Depende de**: [A3.1], para confiar en el certificado; y, antes de arrancar, la decisión
    del owner sobre qué pasa con los datos al desaparear.
  - [ ] **[A2.2] Edición personal sin conexión y fusión a tres bandas.** Room guarda por obra
    la **base** —el estado del servidor en la última sincronización exitosa— y la local
    actual. La base **sólo avanza cuando una sincronización termina entera**: una cortada a
    la mitad no puede dejar el teléfono creyendo que convergió. Incluye la interfaz de
    conflictos, que muestra los dos valores y deja elegir por campo.
    **Servidor**: el `PATCH` de estado personal ya existe.
  - [ ] **[A2.3] Alta sin conexión.** El caso de uso central. Borrador local con id de
    cliente, marcado como no enriquecido, que **no expira**. Al escribirlo, el teléfono avisa
    si se parece a algo que ya tenés, con la normalización del servidor portada ([A3.2]). Es
    un aviso, no una decisión.
    **Servidor: hecho** (`f6e36e1`): `POST /api/v1/catalog/drafts` guarda lo que la persona
    escribió en un borrador por cuenta que no vence, idempotente por el id del cliente, y
    **no enriquece al recibir**. Una obra que el catálogo ya tiene vuelve como `review`, no
    como "ya la tenés": un parecido de título no es identidad.
  - [ ] **[A2.4] Charadas.** Jugar sin red con el mismo mazo que el servidor: el generador
    portado ([A3.3]), el temporizador local y las pantallas de juego. La dificultad **no** se
    calcula en el teléfono: viaja resuelta.
    **Servidor: hecho** (`5cbc34b`): `GET /api/v1/charades` sirve el insumo completo del
    mazo, con las claves tal como las calcula el servidor. Contrato en
    `docs/briefs/charades-v1.md` del servidor.
  - [ ] **[A2.5] Imágenes.** Miniatura local y portada completa en segundo plano, como decidió
    ADR-0005.
  - [ ] **[A2.6] Ampliar lo que viaja**, en el orden del owner: colecciones seguidas, después
    disponibilidad en streaming, después puntajes públicos.
    **Servidor: hecho** (`9ff3ccd`, `7f24895` y `417f6d3`). Reglas de presentación que no se
    pueden romper, y que la web del servidor ya cumple:
    - **Disponibilidad**: una obra ausente del mapa es "no lo consultamos", nunca "no está
      disponible". Sólo la suscripción, lo gratis y lo que tiene publicidad es "disponible";
      alquilar o comprar necesita otro verbo. La atribución a JustWatch es obligatoria. Cada
      fila trae `expires_at`, y **pasado ese momento el teléfono deja de mostrarla**.
    - **Puntajes públicos**: van al lado del propio, nunca en su lugar, ordenados por
      cantidad de votos; con menos de 50 votos se muestran distintos; los de TMDb también
      traen `expires_at`.

  **Lo que sobrevive del brief v1** y sigue vigente: tokens en Android Keystore y nunca en
  preferencias sin cifrar, logs, analytics, URI, portapapeles ni backups; HTTPS con
  certificado válido, con la excepción de `http://10.0.2.2` sólo en `debug`; ignorar campos
  opcionales desconocidos; respetar `X-Movie-Inbox-Api-Version`; y no usar el `/api/`
  histórico, cookies ni `X-Movie-Inbox-Token`.

  **Certificado, medido el 2026-09-07:** `CertificatePinner` de OkHttp **no** sirve para
  aceptar un certificado autofirmado: el pin se comprueba después de un handshake que ya pasó
  por el trust manager, así que un certificado que la plataforma rechaza falla antes. Para
  una instancia con certificado propio hace falta un **trust anchor construido en tiempo de
  ejecución** desde la huella SPKI que trae el QR. Nunca un trust manager permisivo ni
  deshabilitar la verificación de hostname.

### Frente: Lo que se reimplementa del servidor

#### [A3] Portes del servidor

Criterio y detalle en `docs/analisis/lo-que-viene-del-servidor-2026-09-13.md`. La regla: lo
que tiene que dar **exactamente** lo mismo que el servidor se porta contra vectores que el
propio servidor genera y recalcula en sus pruebas; lo demás se mide contra los mismos casos,
sin exigir paridad exacta.

  - [ ] **[A3.1] Leer el QR y confiar en el certificado.** El payload v1 del QR —`v`,
    `origin`, `ticket`, `expires_at`, `instance_id`, `account` y, si la instancia usa
    certificado propio, `certificate_pin`— y el pin SPKI: SHA-256 del
    `SubjectPublicKeyInfo`, en base64, para construir el trust anchor.
    **Depende de**: [X1.1] del servidor, que exporta los vectores del pin. **Para**: [A2.1].
    **Modelo sugerido**: Grande: es seguridad.
  - [ ] **[A3.2] Normalización de títulos y aviso de parecido.** `normalize_search_text`,
    `title_match_key` y `title_similarity` del servidor. Los riesgos del port están en lo que
    Python resuelve solo: `html.unescape`, NFKC y el plegado de diacríticos sólo en letras
    latinas —el japonés conserva sus caracteres—.
    **Depende de**: [X1.2] del servidor. **Para**: [A2.3]. **Modelo sugerido**: Medio.
  - [ ] **[A3.3] Generador de charadas.** FNV-1a de 32 bits, huella, semilla y barajado con el
    LCG documentado. Vectores listos en `contract/charades-v1-vectors.json`, incluida una
    trampa a propósito: las claves se ordenan por punto de código, no por unidad UTF-16, que
    es lo que hace `String.compareTo` en Kotlin. **Para**: [A2.4]. **Modelo sugerido**: Medio.
  - [ ] **[A3.4] Búsqueda sin conexión.** El servidor ordena resultados con
    `search_catalog_items`, el ranking que ajustó [B1] con once arreglos medidos. El teléfono
    busca en su réplica con un ranking propio, medido contra el mismo corpus dorado del
    servidor (`src/movie_inbox/search_lab/corpus/v1.json`: 32 casos con resultados relevantes
    y prohibidos), que se copia a `contract/` al arrancar la tarea. No hace falta paridad
    exacta; pasar los mismos casos, sí. Hasta entonces, un filtro simple por título alcanza
    para [A2.1]. **Modelo sugerido**: Grande.

### Frente: Calidad y entrega

#### [A4] Calidad y entrega

  - [ ] **[A4.1] CI.** `assembleDebug` y pruebas unitarias en GitHub Actions.
    **Depende de**: que exista el repositorio remoto (decisión del owner). **Modelo
    sugerido**: Chico.
  - [ ] **[A4.2] Prueba del contrato.** Que el cliente falle si su modelo de datos se aparta
    de `contract/device-api-v1.openapi.json`, y que actualizar la copia sea un cambio visible.
    **Modelo sugerido**: Medio.
  - [ ] **[A4.3] Subir a `compileSdk` 37.** Instalar la plataforma 37 y pasar a Compose 1.12
    (BOM 2026.08.00) y core 1.19, hoy fijados en versiones anteriores por eso
    (`gradle/libs.versions.toml`). **Modelo sugerido**: Chico.
  - [ ] **[A4.4] Firma y distribución.** Llave de firma fuera del repositorio, `versionCode`
    y cómo llega el APK al teléfono. **Depende de**: tener algo instalable que sirva
    ([A2.1]). **Modelo sugerido**: Medio.

### Decisiones abiertas del owner

1. **Qué pasa con los datos del teléfono si se desaparea.** Recomendación: conservarlos y
   permitir volver a aparear. **Conviene decidirlo antes de [A2.1].**
2. **PIN o biometría propios** además de la pantalla de bloqueo. No frena [A2.1].
3. **Licencia** de este repositorio. El servidor es GPL-3.0.
4. **Repositorio remoto**: nombre y visibilidad en GitHub. Frena [A4.1].

---

## En curso

Nada.

## Hecho

### Frente: Cliente Android

- [x] **[A2.0] Entorno.** Cerrada el 2026-09-13, en el commit inicial de este repositorio.
  Gradle Wrapper 9.7.0, AGP 9.3.1, Kotlin 2.4.10, Compose y `minSdk` 26; `assembleDebug` en
  verde. Sin Hilt ni Room todavía: entran con [A2.1], que es donde se usan. La primera
  corrida dejó dos hallazgos. **Avast interceptaba el HTTPS** hacia `dl.google.com`, Maven
  Central y Gradle, y Java rechaza el certificado que reemite; el owner excluyó esos hosts.
  Y **Compose 1.12 exige `compileSdk` 37**, que no está instalado, así que se fijaron las
  últimas versiones que compilan contra la 36 ([A4.3]). El proyecto se creó primero, por
  error, dentro del repositorio del servidor, contra lo decidido el 2026-08-17; se mudó acá
  antes de publicarlo.

### Lo que el servidor ya construyó para este cliente

Referencia, con commits del repositorio del servidor:

- **API de dispositivo v1** ([A1]): contrato congelado (`34c2c24`), sesiones revocables por
  dispositivo (`73e75f5`), catálogo, detalle, búsqueda y edición personal (`fa217e2`) e ids
  de obra estables (`609f56e`).
- **Apareamiento** ([A2.1]): `334f4fe`, `e9bcaab` y `11dbe0b`.
- **Alta sin conexión** ([A2.3]): `f6e36e1`.
- **Charadas** ([G2] y [A2.4]): `72d1f31` y `5cbc34b`.
- **Lo que viaja** ([A2.6]): `9ff3ccd`, `7f24895` y `417f6d3`.
