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
| 1 | [A5] | Probar la sincronización y la fusión en cada caso de una misma cuenta usada por separado, antes de construir pantallas | — |
| 2 | [A2.1] | Aparear y ver el catálogo real sin conexión | [A3.1] |
| 3 | [A2.2] | Editar el estado personal sin conexión, con la fusión que probó [A5] | [A2.1]; [A5] |
| 4 | [A2.3] | Dar de alta obras sin conexión | [A2.1]; [A3.2] |
| 5 | [A2.4] | Charadas sin conexión, con el mismo mazo que el servidor | [A2.1]; [A3.3] |
| 6 | [A2.5] | Imágenes: miniatura local y portada en segundo plano | [A2.1] |
| 7 | [A2.6] | Colecciones seguidas, disponibilidad y puntajes públicos | [A2.1] |
| 8 | [A3.4] | Búsqueda sin conexión con el ranking medido contra el corpus del servidor | [A2.1] |
| — | [A4] | Calidad y entrega: CI, prueba del contrato, `compileSdk` 37, firma | se toma cuando haga falta |

**Por qué la sincronización va primero**, por decisión del owner del 2026-09-13: es la parte
que puede obligar a cambiar el contrato con el servidor, y descubrirlo después de construir
pantallas sale caro. Se prueba sin interfaz y sin apareamiento.

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
    **Depende de**: [A3.1], para confiar en el certificado. Al desaparear, los datos del
    teléfono **persisten**, por decisión del owner del 2026-09-13.
  - [ ] **[A2.2] Edición personal sin conexión y fusión a tres bandas.** Room guarda por obra
    la **base** —el estado del servidor en la última sincronización exitosa— y la local
    actual. La base **sólo avanza cuando una sincronización termina entera**: una cortada a
    la mitad no puede dejar el teléfono creyendo que convergió. Incluye la interfaz de
    conflictos, que muestra los dos valores y deja elegir por campo.
    **Servidor**: el `PATCH` de estado personal ya existe, sin precondición; si [A5.1]
    confirma que hace falta una, es [X2] del servidor. Las reglas y los casos salen de [A5].
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

### Frente: Sincronización

#### [A5] Probar la sincronización y la fusión antes de construir pantallas

Pedido del owner el 2026-09-13: lo primero es saber **cómo se sincroniza** y **qué pasa en
cada caso en que una misma cuenta se usa por separado** —la web y un teléfono, o dos
teléfonos—, y cómo se arma la fusión. Se prueba sin interfaz y sin apareamiento: sesiones de
dispositivo por `POST /api/v1/auth/login` contra una instancia descartable del servidor, con
un catálogo sintético.

**Lo que ya se sabe del servidor**, leído en su código el 2026-09-13: `PATCH
/api/v1/catalog/items/{id}/personal` aplica los valores tal como llegan, sin precondición.
Si la web u otro teléfono cambió el mismo campo entre que este teléfono bajó el estado y lo
subió, **gana el último y el otro cambio se pierde sin aviso**. La fusión a tres bandas del
teléfono no alcanza para evitarlo: decide con lo que bajó, no con lo que hay en el servidor
en el momento de subir.

  - [x] **[A5.1] Matriz de casos.** Cada caso con su resultado esperado, lo que hace hoy el
    servidor y si hace falta cambiar algo. Como mínimo:
    1. El teléfono edita sin red y la web no toca esa obra.
    2. La web edita y el teléfono no.
    3. Los dos cambian el mismo campo al mismo valor.
    4. Los dos cambian el mismo campo a valores distintos: decide la persona.
    5. Cambian campos distintos de la misma obra.
    6. Dos teléfonos con la misma cuenta, sincronizando uno después del otro.
    7. Una edición en la web, o en otro teléfono, entre que el teléfono baja y sube.
    8. Una sincronización cortada a la mitad, y su reintento.
    9. Una obra que en el servidor se fusionó con un duplicado o se borró, con cambios
       pendientes en el teléfono.
    10. La misma obra dada de alta sin conexión en dos teléfonos, o una que ya existía.
    11. Desaparear con cambios pendientes —los datos persisten—, volver a aparear con la misma
        cuenta, y aparear con **otra** cuenta, que nunca puede mezclar datos de las dos.
    12. Sesión revocada o contraseña cambiada con cambios pendientes.
    13. Campos que se afectan entre sí: `status` y `watched_at`; puntaje en 0 y puntaje vacío;
        review vacía y review ausente.
    **Modelo sugerido**: Grande.
    **Cerrada el 2026-09-14**, en `docs/analisis/matriz-de-sincronizacion-2026-09-14.md`,
    leída en el servidor en `83a5cf4`. Salieron 18 casos: los 13 pedidos y 5 encontrados en el
    código. Dos pierden un cambio sin aviso:
    - el 7, que confirma [X2];
    - el 14, una ficha web abierta desde antes de sincronizar.

    Además dejó:
    - doce reglas para [A5.2];
    - ocho cambios de servidor para [A5.4];
    - seis decisiones del owner, entre ellas la redacción del invariante 3, que leído al pie de
      la letra pierde datos.
  - [ ] **[A5.2] Reglas de fusión como código puro.** Un módulo Kotlin sin Android, con una
    prueba por fila de la matriz: por campo, qué pasa si cambió un lado, los dos igual o los
    dos distinto, y cómo se presenta un conflicto. **Modelo sugerido**: Grande.
  - [ ] **[A5.3] Arnés contra un servidor real.** Dos o más clientes simulados con la misma
    cuenta, contra una instancia descartable, recorriendo los casos de [A5.1] y comprobando que
    convergen o que el conflicto le llega a la persona. Es lo que valida que las reglas de
    [A5.2] describen al servidor verdadero y no a uno imaginado. **Modelo sugerido**: Grande.
  - [ ] **[A5.4] Lo que el servidor tenga que cambiar.** Lo que muestre la matriz, empezando
    por la precondición del `PATCH` —[X2] del servidor—, anotado en el tablero de allá antes
    de construir [A2.2]. **Modelo sugerido**: Medio.
  - **Decisiones que salen de acá**: seis, con su recomendación, en la sección "Decisiones
    para el owner" de la matriz. El owner respondió el 2026-09-14 (sección "Respuestas del
    owner"), y quedan abiertas la 2, la 4 y la 5. [A5.2] puede arrancar con las reglas que no
    dependen de ellas.

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
    El repositorio remoto ya existe. **Modelo sugerido**: Chico.
  - [ ] **[A4.2] Prueba del contrato.** Que el cliente falle si su modelo de datos se aparta
    de `contract/device-api-v1.openapi.json`, y que actualizar la copia sea un cambio visible.
    **Modelo sugerido**: Medio.
  - [ ] **[A4.3] Subir a `compileSdk` 37.** Instalar la plataforma 37 y pasar a Compose 1.12
    (BOM 2026.08.00) y core 1.19, hoy fijados en versiones anteriores por eso
    (`gradle/libs.versions.toml`). **Modelo sugerido**: Chico.
  - [ ] **[A4.4] Firma y distribución.** Llave de firma fuera del repositorio, `versionCode`
    y cómo llega el APK al teléfono. **Depende de**: tener algo instalable que sirva
    ([A2.1]). **Modelo sugerido**: Medio.

### Decisiones del owner

**Tomadas el 2026-09-13:**

- **Al desaparear, los datos del teléfono persisten.**
- **La sincronización se prueba primero**, antes de construir pantallas: [A5].
- **Licencia GPL-3.0**, la misma que el servidor.
- **Repositorio remoto**: `github.com/pasaporteN25/MovieCacheAndroid`.

**Tomadas el 2026-09-14**, al responder la matriz de [A5.1]:

- **Con el teléfono desapareado se puede seguir editando**, y los cambios viajan al volver a
  aparear la misma cuenta.
- **Para cambiar de cuenta, primero se sincroniza la actual.** Mejorarlo más adelante queda
  como idea.
- **La sesión de un teléfono no vence por tiempo**, y volver a aparear no puede dar problemas:
  [X4] del servidor.
- **El servidor registra cuándo cambia cada campo personal**, en su backlog: [X3] del
  servidor. Informa en un conflicto; no lo decide.

**Abiertas:**

- PIN o biometría propios, además de la pantalla de bloqueo. No frena [A2.1].
- Cómo ve la persona un conflicto (decisión 2 de la matriz).
- La redacción del invariante 3 sobre la base (decisión 4).
- Si borrar una obra en un lado la borra en el otro al sincronizar (decisión 5). El owner se
  inclina por que sí; como revierte ADR-0005 (§3) y el invariante 3, se confirma con sus
  condiciones antes de anotarla.

---

## En curso

- **[A5]**: [A5.1] quedó cerrada el 2026-09-14, y el owner ya resolvió la 1, la 3 y la 6 de sus
  decisiones. Siguen la 2, la 4 y la 5, y [A5.2].

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
