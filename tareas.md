# Tareas â€” movieIndexAndroid

Tablero en Markdown, versionado en el repo, con las mismas reglas que el del servidor.
Columnas = estado (`Backlog` / `En curso` / `Hecho`). Las tareas se agrupan por frente, con
alcance concreto, dependencias explÃ­citas y un nivel de modelo sugerido: mecÃ¡nica de un solo
archivo sin decisiones â†’ chico; pocas piezas con las decisiones ya tomadas â†’ medio; requiere
criterio, coordina varias piezas o toca identidad, sincronizaciÃ³n o seguridad â†’ grande. Al
cerrar una tarea: moverla a `Hecho` con fecha y commit, no borrarla.

**Origen.** Este tablero arrancÃ³ el 2026-09-13 con la Ã©pica [A2], que hasta ese dÃ­a vivÃ­a en
el `tareas.md` del servidor. Los nÃºmeros [A2.x] se conservan porque los commits del servidor
los citan; las Ã©picas nuevas de este repo siguen la serie: [A3], [A4]. Lo que el servidor
tiene que hacer para este cliente vive en el tablero del servidor, y acÃ¡ se cita con su
nÃºmero de allÃ¡, como [X1.1].

---

## Backlog

## Resumen operativo

**PreparaciÃ³n de emparejamiento, 2026-10-04:** plan de entrega y gates en
[`mobile-pairing-v1.md`](docs/briefs/mobile-pairing-v1.md). Primero P0 y el primer
caso Kotlin/servidor real de P1 (A5.2/A5.3); despuÃ©s pairing y rÃ©plica offline.
Compose + MVVM, View Binding si se necesita XML. Dump y dispositivo se usan en
QA manual aislado; no bloquean el arnÃ©s con datos sintÃ©ticos. Build debug
verificado hoy, app aÃºn placeholder y ningÃºn dispositivo conectado.
X1.1/X1.2 del servidor ya estÃ¡n cerrados: actualizar las copias de sus vectores
en P0. Las dependencias histÃ³ricas de abajo no implican que sigan pendientes.

**Objetivo del owner, 2026-10-04:** Android es el prÃ³ximo gran frente: capturar
recomendaciones de redes/conversaciones y buscar obras, directores y actores con
paridad de capacidades con la web. Descubrimiento SDD en
[`docs/briefs/daily-discovery-v1.md`](docs/briefs/daily-discovery-v1.md); no supone
funciones ya construidas. Primero alinear contrato y probar sincronizaciÃ³n A5.2/A5.3.
X1â€“X10 ya estÃ¡n en master del servidor; X11 estÃ¡ hecho en su release 0.11.0 y es
requisito de M2. La pantalla del QR sigue pendiente. Esta nota prevalece sobre las
dependencias histÃ³ricas que aÃºn dicen Â«si la matriz confirmaÂ».

| Orden | Tarea | Resultado esperado | Depende de |
| --- | --- | --- | --- |
| 1 | [A5] | Probar la sincronizaciÃ³n y la fusiÃ³n en cada caso de una misma cuenta usada por separado, antes de construir pantallas | â€” |
| 2 | [A2.1] | Aparear y ver el catÃ¡logo real sin conexiÃ³n | [A3.1] |
| 3 | [A2.2] | Editar el estado personal sin conexiÃ³n, con la fusiÃ³n que probÃ³ [A5] | [A2.1]; [A5] |
| 4 | [A2.3] | Dar de alta obras sin conexiÃ³n | [A2.1]; [A3.2] |
| 5 | [A2.4] | Charadas sin conexiÃ³n, con el mismo mazo que el servidor | [A2.1]; [A3.3] |
| 6 | [A2.5] | ImÃ¡genes: miniatura local y portada en segundo plano | [A2.1] |
| 7 | [A2.6] | Colecciones seguidas, disponibilidad y puntajes pÃºblicos | [A2.1] |
| 8 | [A3.4] | BÃºsqueda sin conexiÃ³n con el ranking medido contra el corpus del servidor | [A2.1] |
| â€” | [A4] | Calidad y entrega: CI, prueba del contrato, `compileSdk` 37, firma | se toma cuando haga falta |

**Por quÃ© la sincronizaciÃ³n va primero**, por decisiÃ³n del owner del 2026-09-13: es la parte
que puede obligar a cambiar el contrato con el servidor, y descubrirlo despuÃ©s de construir
pantallas sale caro. Se prueba sin interfaz y sin apareamiento.

**Para probar [A2.1] de punta a punta** hacen falta dos cosas que no son de este repo: la
pantalla del servidor que genera el QR, que es un traspaso al frente visual de allÃ¡, y el
certificado de la red local, que emite el owner con la receta de `docs/deployment.md` del
servidor.

### Frente: Cliente Android

#### [A2] Cliente Android autÃ³nomo

- **Alcance**: aplicaciÃ³n Android nativa (Kotlin + Compose) con almacÃ©n local propio. Se
  aparea una vez con una cuenta que ya existe en la instancia, y **desde ahÃ­ funciona sin
  conexiÃ³n**: explorar, buscar, editar estado personal y **dar de alta obras nuevas**. La
  sincronizaciÃ³n es bidireccional y la inicia una persona. Un borrado viaja sÃ³lo como registro
  de que una persona borrÃ³; que una obra falte **nunca borra nada**.
- **Caso de uso que manda**, dicho por el owner: *guardar pelÃ­culas en la colecciÃ³n sin estar
  frente a la computadora ni en casa*. Todo el orden de entrega sale de ahÃ­.
- **Criterio de cierre**: el telÃ©fono muestra y edita el catÃ¡logo real sin conexiÃ³n, y una
  obra dada de alta sin red llega al servidor por el camino de importaciÃ³n y revisiÃ³n que ya
  existe, sin decidir identidad por su cuenta.
- **Plan vigente**: `docs/briefs/android-client-v3.md`. DirecciÃ³n: ADR-0005 del servidor,
  enmendada por la decisiÃ³n de la cuenta obligatoria.
- **Modelo sugerido**: Grande. Es un proyecto cliente completo, no una pantalla mÃ¡s.

**La distinciÃ³n que sostiene el diseÃ±o**: editar una obra que ya existe de los dos lados
tiene **base compartida** y se resuelve por fusiÃ³n a tres bandas; dar de alta una obra que no
existe en ningÃºn lado **no tiene base**, asÃ­ que no es una fusiÃ³n sino una **importaciÃ³n**, y
va por el camino de revisiÃ³n que ya existe. Confundirlas obligarÃ­a a inventar una base.

  - [ ] **[A2.1] Esqueleto, apareamiento y lectura sin conexiÃ³n.** El primer hito pedido por
    el owner: **aparear y ver el catÃ¡logo real en el telÃ©fono**. Hilt con KSP, coroutines y
    `StateFlow`; el repositorio es el lÃ­mite de errores. Room con la forma del contrato
    portable v9 â€”capa de obra y capa personal, **nunca** la operativaâ€”. Escaneo del QR con
    ZXing embebido, para no arrastrar Play Services. Descarga completa, paginada por cursor.
    **Servidor: hecho** â€”ticket de un solo uso, canje en `POST /api/v1/pair`, QR dibujado en
    el servidor y pin derivado del certificado; commits `334f4fe`, `e9bcaab` y `11dbe0b`â€”,
    salvo la pantalla que muestra el QR.
    **Depende de**: [A3.1], para confiar en el certificado. Al desaparear, los datos del
    telÃ©fono **persisten**, por decisiÃ³n del owner del 2026-09-13.
  - [ ] **[A2.2] EdiciÃ³n personal sin conexiÃ³n y fusiÃ³n a tres bandas.** Room guarda por obra
    la **base** â€”el estado del servidor en la Ãºltima sincronizaciÃ³n exitosaâ€” y la local
    actual. La base **sÃ³lo avanza cuando una sincronizaciÃ³n termina entera**: una cortada a
    la mitad no puede dejar el telÃ©fono creyendo que convergiÃ³. Incluye la interfaz de
    conflictos, que muestra los dos valores y deja elegir por campo.
    **Servidor**: el `PATCH` de estado personal ya existe, sin precondiciÃ³n; si [A5.1]
    confirma que hace falta una, es [X2] del servidor. Las reglas y los casos salen de [A5].
  - [ ] **[A2.3] Alta sin conexiÃ³n.** El caso de uso central. Borrador local con id de
    cliente, marcado como no enriquecido, que **no expira**. Al escribirlo, el telÃ©fono avisa
    si se parece a algo que ya tenÃ©s, con la normalizaciÃ³n del servidor portada ([A3.2]). Es
    un aviso, no una decisiÃ³n.
    **Servidor: hecho** (`f6e36e1`): `POST /api/v1/catalog/drafts` guarda lo que la persona
    escribiÃ³ en un borrador por cuenta que no vence, idempotente por el id del cliente, y
    **no enriquece al recibir**. Una obra que el catÃ¡logo ya tiene vuelve como `review`, no
    como "ya la tenÃ©s": un parecido de tÃ­tulo no es identidad.
  - [ ] **[A2.4] Charadas.** Jugar sin red con el mismo mazo que el servidor: el generador
    portado ([A3.3]), el temporizador local y las pantallas de juego. La dificultad **no** se
    calcula en el telÃ©fono: viaja resuelta.
    **Servidor: hecho** (`5cbc34b`): `GET /api/v1/charades` sirve el insumo completo del
    mazo, con las claves tal como las calcula el servidor. Contrato en
    `docs/briefs/charades-v1.md` del servidor.
  - [ ] **[A2.5] ImÃ¡genes.** Miniatura local y portada completa en segundo plano, como decidiÃ³
    ADR-0005.
  - [ ] **[A2.6] Ampliar lo que viaja**, en el orden del owner: colecciones seguidas, despuÃ©s
    disponibilidad en streaming, despuÃ©s puntajes pÃºblicos.
    **Servidor: hecho** (`9ff3ccd`, `7f24895` y `417f6d3`). Reglas de presentaciÃ³n que no se
    pueden romper, y que la web del servidor ya cumple:
    - **Disponibilidad**: una obra ausente del mapa es "no lo consultamos", nunca "no estÃ¡
      disponible". SÃ³lo la suscripciÃ³n, lo gratis y lo que tiene publicidad es "disponible";
      alquilar o comprar necesita otro verbo. La atribuciÃ³n a JustWatch es obligatoria. Cada
      fila trae `expires_at`, y **pasado ese momento el telÃ©fono deja de mostrarla**.
    - **Puntajes pÃºblicos**: van al lado del propio, nunca en su lugar, ordenados por
      cantidad de votos; con menos de 50 votos se muestran distintos; los de TMDb tambiÃ©n
      traen `expires_at`.

  **Lo que sobrevive del brief v1** y sigue vigente: tokens en Android Keystore y nunca en
  preferencias sin cifrar, logs, analytics, URI, portapapeles ni backups; HTTPS con
  certificado vÃ¡lido, con la excepciÃ³n de `http://10.0.2.2` sÃ³lo en `debug`; ignorar campos
  opcionales desconocidos; respetar `X-Movie-Inbox-Api-Version`; y no usar el `/api/`
  histÃ³rico, cookies ni `X-Movie-Inbox-Token`.

  **Certificado, medido el 2026-09-07:** `CertificatePinner` de OkHttp **no** sirve para
  aceptar un certificado autofirmado: el pin se comprueba despuÃ©s de un handshake que ya pasÃ³
  por el trust manager, asÃ­ que un certificado que la plataforma rechaza falla antes. Para
  una instancia con certificado propio hace falta un **trust anchor construido en tiempo de
  ejecuciÃ³n** desde la huella SPKI que trae el QR. Nunca un trust manager permisivo ni
  deshabilitar la verificaciÃ³n de hostname.

### Frente: SincronizaciÃ³n

#### [A5] Probar la sincronizaciÃ³n y la fusiÃ³n antes de construir pantallas

Pedido del owner el 2026-09-13: lo primero es saber **cÃ³mo se sincroniza** y **quÃ© pasa en
cada caso en que una misma cuenta se usa por separado** â€”la web y un telÃ©fono, o dos
telÃ©fonosâ€”, y cÃ³mo se arma la fusiÃ³n. Se prueba sin interfaz y sin apareamiento: sesiones de
dispositivo por `POST /api/v1/auth/login` contra una instancia descartable del servidor, con
un catÃ¡logo sintÃ©tico.

**Lo que ya se sabe del servidor**, leÃ­do en su cÃ³digo el 2026-09-13: `PATCH
/api/v1/catalog/items/{id}/personal` aplica los valores tal como llegan, sin precondiciÃ³n.
Si la web u otro telÃ©fono cambiÃ³ el mismo campo entre que este telÃ©fono bajÃ³ el estado y lo
subiÃ³, **gana el Ãºltimo y el otro cambio se pierde sin aviso**. La fusiÃ³n a tres bandas del
telÃ©fono no alcanza para evitarlo: decide con lo que bajÃ³, no con lo que hay en el servidor
en el momento de subir.

  - [x] **[A5.1] Matriz de casos.** Cada caso con su resultado esperado, lo que hace hoy el
    servidor y si hace falta cambiar algo. Como mÃ­nimo:
    1. El telÃ©fono edita sin red y la web no toca esa obra.
    2. La web edita y el telÃ©fono no.
    3. Los dos cambian el mismo campo al mismo valor.
    4. Los dos cambian el mismo campo a valores distintos: decide la persona.
    5. Cambian campos distintos de la misma obra.
    6. Dos telÃ©fonos con la misma cuenta, sincronizando uno despuÃ©s del otro.
    7. Una ediciÃ³n en la web, o en otro telÃ©fono, entre que el telÃ©fono baja y sube.
    8. Una sincronizaciÃ³n cortada a la mitad, y su reintento.
    9. Una obra que en el servidor se fusionÃ³ con un duplicado o se borrÃ³, con cambios
       pendientes en el telÃ©fono.
    10. La misma obra dada de alta sin conexiÃ³n en dos telÃ©fonos, o una que ya existÃ­a.
    11. Desaparear con cambios pendientes â€”los datos persistenâ€”, volver a aparear con la misma
        cuenta, y aparear con **otra** cuenta, que nunca puede mezclar datos de las dos.
    12. SesiÃ³n revocada o contraseÃ±a cambiada con cambios pendientes.
    13. Campos que se afectan entre sÃ­: `status` y `watched_at`; puntaje en 0 y puntaje vacÃ­o;
        review vacÃ­a y review ausente.
    **Modelo sugerido**: Grande.
    **Cerrada el 2026-09-14**, en `docs/analisis/matriz-de-sincronizacion-2026-09-14.md`,
    leÃ­da en el servidor en `83a5cf4`. Salieron 18 casos: los 13 pedidos y 5 encontrados en el
    cÃ³digo. Dos pierden un cambio sin aviso:
    - el 7, que confirma [X2];
    - el 14, una ficha web abierta desde antes de sincronizar.

    AdemÃ¡s dejÃ³:
    - doce reglas para [A5.2];
    - ocho cambios de servidor para [A5.4];
    - seis decisiones del owner, entre ellas la redacciÃ³n del invariante 3, que leÃ­do al pie de
      la letra pierde datos.
  - [ ] **[A5.2] Reglas de fusiÃ³n como cÃ³digo puro.** Un mÃ³dulo Kotlin sin Android, con una
    prueba por fila de la matriz: por campo, quÃ© pasa si cambiÃ³ un lado, los dos igual o los
    dos distinto, y cÃ³mo se presenta un conflicto. **Modelo sugerido**: Grande.
  - [ ] **[A5.3] ArnÃ©s contra un servidor real.** Dos o mÃ¡s clientes simulados con la misma
    cuenta, contra una instancia descartable, recorriendo los casos de [A5.1] y comprobando que
    convergen o que el conflicto le llega a la persona. Es lo que valida que las reglas de
    [A5.2] describen al servidor verdadero y no a uno imaginado. **Modelo sugerido**: Grande.
  - [ ] **[A5.4] Lo que el servidor tenga que cambiar.** Lo que muestre la matriz, empezando
    por la precondiciÃ³n del `PATCH` â€”[X2] del servidorâ€”, anotado en el tablero de allÃ¡ antes
    de construir [A2.2]. **Modelo sugerido**: Medio.
  - **Decisiones que salen de acÃ¡**: seis, con su recomendaciÃ³n, en la secciÃ³n "Decisiones
    para el owner" de la matriz. El owner las respondiÃ³ todas el 2026-09-14, en tres rondas
    (secciones "Respuestas del owner", "Segunda ronda" y "Tercera ronda").

### Frente: Lo que se reimplementa del servidor

#### [A3] Portes del servidor

Criterio y detalle en `docs/analisis/lo-que-viene-del-servidor-2026-09-13.md`. La regla: lo
que tiene que dar **exactamente** lo mismo que el servidor se porta contra vectores que el
propio servidor genera y recalcula en sus pruebas; lo demÃ¡s se mide contra los mismos casos,
sin exigir paridad exacta.

  - [ ] **[A3.1] Leer el QR y confiar en el certificado.** El payload v1 del QR â€”`v`,
    `origin`, `ticket`, `expires_at`, `instance_id`, `account` y, si la instancia usa
    certificado propio, `certificate_pin`â€” y el pin SPKI: SHA-256 del
    `SubjectPublicKeyInfo`, en base64, para construir el trust anchor.
    **Depende de**: [X1.1] del servidor, que exporta los vectores del pin. **Para**: [A2.1].
    **Modelo sugerido**: Grande: es seguridad.
  - [ ] **[A3.2] NormalizaciÃ³n de tÃ­tulos y aviso de parecido.** `normalize_search_text`,
    `title_match_key` y `title_similarity` del servidor. Los riesgos del port estÃ¡n en lo que
    Python resuelve solo: `html.unescape`, NFKC y el plegado de diacrÃ­ticos sÃ³lo en letras
    latinas â€”el japonÃ©s conserva sus caracteresâ€”.
    **Depende de**: [X1.2] del servidor. **Para**: [A2.3]. **Modelo sugerido**: Medio.
  - [ ] **[A3.3] Generador de charadas.** FNV-1a de 32 bits, huella, semilla y barajado con el
    LCG documentado. Vectores listos en `contract/charades-v1-vectors.json`, incluida una
    trampa a propÃ³sito: las claves se ordenan por punto de cÃ³digo, no por unidad UTF-16, que
    es lo que hace `String.compareTo` en Kotlin. **Para**: [A2.4]. **Modelo sugerido**: Medio.
  - [ ] **[A3.4] BÃºsqueda sin conexiÃ³n.** El servidor ordena resultados con
    `search_catalog_items`, el ranking que ajustÃ³ [B1] con once arreglos medidos. El telÃ©fono
    busca en su rÃ©plica con un ranking propio, medido contra el mismo corpus dorado del
    servidor (`src/movie_inbox/search_lab/corpus/v1.json`: 32 casos con resultados relevantes
    y prohibidos), que se copia a `contract/` al arrancar la tarea. No hace falta paridad
    exacta; pasar los mismos casos, sÃ­. Hasta entonces, un filtro simple por tÃ­tulo alcanza
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
  - [ ] **[A4.4] Firma y distribuciÃ³n.** Llave de firma fuera del repositorio, `versionCode`
    y cÃ³mo llega el APK al telÃ©fono. **Depende de**: tener algo instalable que sirva
    ([A2.1]). **Modelo sugerido**: Medio.

### Decisiones del owner

**Tomadas el 2026-09-13:**

- **Al desaparear, los datos del telÃ©fono persisten.**
- **La sincronizaciÃ³n se prueba primero**, antes de construir pantallas: [A5].
- **Licencia GPL-3.0**, la misma que el servidor.
- **Repositorio remoto**: `github.com/pasaporteN25/MovieCacheAndroid`.

**Tomadas el 2026-09-14**, al responder la matriz de [A5.1] en dos rondas:

- **Con el telÃ©fono desapareado se puede seguir editando**, y los cambios viajan al volver a
  aparear la misma cuenta.
- **Para cambiar de cuenta, primero se sincroniza la actual.** Mejorarlo mÃ¡s adelante queda
  como idea.
- **El servidor registra cuÃ¡ndo cambia cada campo personal**, en su backlog: [X3] del
  servidor. Informa en un conflicto; no lo decide.
- **Un conflicto de puntaje se resuelve solo, por el mÃ¡s alto.** Es asÃ­ por definiciÃ³n, no por
  una limitaciÃ³n, y se puede cambiar mÃ¡s adelante; no es prioridad.
- **La base de un campo sÃ³lo avanza con un valor que confirmÃ³ el servidor**, en la misma
  transacciÃ³n que la rÃ©plica: es el invariante 3. Si esa regla trae otro problema, la
  alternativa anotada es descartar la sincronizaciÃ³n que quedÃ³ a medias y pedir que se haga de
  nuevo, que al owner le parece mejor y mÃ¡s escalable a futuro.
- **Los borrados viajan**, con tres condiciones:
  - un borrado viaja sÃ³lo como registro de que una persona borrÃ³, y que una obra falte nunca
    borra nada;
  - si un lado borrÃ³ y el otro editÃ³ la misma obra, decide la persona;
  - unir duplicados en la web cuenta como borrar el duplicado, y lo pendiente pasa a la obra
    que queda.

  Revierte "nunca borra" de ADR-0005 y es [X5] del servidor. Para su diseÃ±o, el owner sugiere
  seguir cada obra con un identificador propio y durable.
- **La llave de un telÃ©fono vence, y pasado un mes hay que volver a escanear el QR.** Revisa
  el "no vence" de la primera ronda, por seguridad, y podrÃ­a pedirse mÃ¡s seguido. El mes se
  cuenta desde la Ãºltima sincronizaciÃ³n, como hoy. [X4] del servidor sigue, sin el cambio de
  vencimiento.
- **Los conflictos de review y de "visto" tambiÃ©n se resuelven solos.** La review conserva los
  dos textos, uno debajo del otro. En "visto", "vista" gana sobre "pendiente", y entre dos
  fechas queda la mÃ¡s reciente. El Ãºnico conflicto que llega a la persona es un borrado
  contra una ediciÃ³n.

**Abierta:** PIN o biometrÃ­a propios, ademÃ¡s de la pantalla de bloqueo. No frena [A2.1].

---

## En curso

- **Primer corte P0/P1, 2026-10-05:** contrato/vectores copiados del servidor
  `72954fe` con hashes; reglas Kotlin y ciclo HTTP del puntaje implementados.
  Evidencia y comando en [sync-rating-first-cut.md](docs/briefs/sync-rating-first-cut.md).
  12 pruebas y APK debug verdes. A5.2/A5.3 siguen abiertas: faltan los demás campos
  y escenarios. Room y pairing aún pendientes; ninguna pantalla nueva.


- **[A5]**: [A5.1] quedÃ³ cerrada el 2026-09-14, con todas sus decisiones respondidas por el
  owner. Sigue [A5.2].

## Hecho

### Frente: Cliente Android

- [x] **[A2.0] Entorno.** Cerrada el 2026-09-13, en el commit inicial de este repositorio.
  Gradle Wrapper 9.7.0, AGP 9.3.1, Kotlin 2.4.10, Compose y `minSdk` 26; `assembleDebug` en
  verde. Sin Hilt ni Room todavÃ­a: entran con [A2.1], que es donde se usan. La primera
  corrida dejÃ³ dos hallazgos. **Avast interceptaba el HTTPS** hacia `dl.google.com`, Maven
  Central y Gradle, y Java rechaza el certificado que reemite; el owner excluyÃ³ esos hosts.
  Y **Compose 1.12 exige `compileSdk` 37**, que no estÃ¡ instalado, asÃ­ que se fijaron las
  Ãºltimas versiones que compilan contra la 36 ([A4.3]). El proyecto se creÃ³ primero, por
  error, dentro del repositorio del servidor, contra lo decidido el 2026-08-17; se mudÃ³ acÃ¡
  antes de publicarlo.

### Lo que el servidor ya construyÃ³ para este cliente

Referencia, con commits del repositorio del servidor:

- **API de dispositivo v1** ([A1]): contrato congelado (`34c2c24`), sesiones revocables por
  dispositivo (`73e75f5`), catÃ¡logo, detalle, bÃºsqueda y ediciÃ³n personal (`fa217e2`) e ids
  de obra estables (`609f56e`).
- **Apareamiento** ([A2.1]): `334f4fe`, `e9bcaab` y `11dbe0b`.
- **Alta sin conexiÃ³n** ([A2.3]): `f6e36e1`.
- **Charadas** ([G2] y [A2.4]): `72d1f31` y `5cbc34b`.
- **Lo que viaja** ([A2.6]): `9ff3ccd`, `7f24895` y `417f6d3`.
