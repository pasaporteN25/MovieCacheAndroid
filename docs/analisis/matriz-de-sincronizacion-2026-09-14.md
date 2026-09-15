# Sincronización: matriz de casos

**Fecha:** 2026-09-14. **Tarea:** [A5.1]. **Leído en:** el servidor en `83a5cf4` (rama
`release/0.9.0`) y el contrato de `contract/`, idéntico al del servidor en esa versión. Las
rutas `src/...`, `docs/...` y `scripts/...` son del repositorio del servidor.

**Qué es.** El pedido del owner del 2026-09-13: antes de construir pantallas, saber qué pasa
en cada caso en que una misma cuenta se usa por separado —la web y un teléfono, o dos
teléfonos—. Cada caso dice qué debería pasar, qué hace hoy el servidor según su código y qué
hace falta: una regla del cliente, que va a [A5.2]; un cambio del servidor, que se anota allá
en [A5.4]; o una decisión del owner.

**Qué no es.** Nada de esto corrió todavía contra un servidor: eso es [A5.3], que confirma o
desmiente cada fila.

## En una tabla

| # | Caso | Hoy | Qué hace falta |
| --- | --- | --- | --- |
| 1 | El teléfono edita sin red; la web no toca la obra | Converge con dos reglas | Cliente |
| 2 | La web edita; el teléfono no | Converge | Cliente |
| 3 | Los dos cambian un campo al mismo valor | Converge | Cliente: normalizar antes de comparar |
| 4 | Los dos cambian un campo a valores distintos | Conflicto para la persona | Owner: cómo lo ve |
| 5 | Campos distintos de la misma obra | Converge, salvo `status` con `watched_at` | Cliente: esos dos van juntos |
| 6 | Dos teléfonos, uno después del otro | Converge | — |
| 7 | La web u otro teléfono edita entre que este baja y sube | **Pierde un cambio sin aviso** | Servidor: [X2] |
| 8 | Sincronización cortada y reintento | Converge, salvo en dos cortes | Servidor; owner: el invariante 3 |
| 9 | Obra fusionada o borrada en el servidor, con cambios en el teléfono | 404 sin explicación | Cliente; owner; servidor a futuro |
| 10 | Misma obra dada de alta en dos teléfonos, o una que ya existía | Queda en revisión; el teléfono nunca se entera del resultado | Servidor: recibos de altas |
| 11 | Desaparear, volver a aparear, aparear otra cuenta | Converge con reglas | Cliente; owner |
| 12 | Sesión revocada o contraseña cambiada, con cambios pendientes | Converge con reglas | Cliente; owner |
| 13 | Campos que se afectan entre sí | Converge con reglas | Cliente |
| 14 | Una ficha abierta en la web desde antes de sincronizar | **Pierde un cambio sin aviso** | Servidor y frente visual |
| 15 | El servidor se reinicia en medio de la descarga | La descarga falla | Cliente; servidor, opcional |
| 16 | Altas, bajas o títulos que cambian durante la descarga | Puede saltear o repetir obras | Cliente; servidor, opcional |
| 17 | Cambia la posición de una fuente del catálogo | Cambian todos los ids | Cliente: detectarlo y frenar |
| 18 | Dos sincronizaciones a la vez en el mismo teléfono | Puede perder la sesión | Cliente |

Del 1 al 13 son los que pidió [A5.1]. Del 14 al 18 aparecieron al leer el código.

## Cómo sincroniza un teléfono con la API de hoy

La API no tiene marcas de tiempo, versiones ni feed de cambios, y es a propósito (ADR-0005,
§6.1): la fusión a tres bandas no los necesita, porque la base vive en el teléfono. Con eso,
una sincronización es:

1. **Una sola a la vez** por instalación (caso 18).
2. **Bajar la réplica entera** con `GET /api/v1/catalog/items`, página por página hasta que
   `next_cursor` venga vacío, sin tocar todavía ni la réplica ni la base. Si una página falla
   con 400, se empieza de nuevo (caso 15). Una obra repetida cuenta una vez (caso 16).
3. **Subir las altas** con `POST /api/v1/catalog/drafts`, de a 100 como máximo, con el id que
   generó el teléfono.
4. **Fusionar por obra y por campo** contra la base, con la tabla de ADR-0005, §4:
   - sólo cambió el servidor: se toma su valor, y la réplica y la base avanzan juntas;
   - sólo cambió el teléfono: se sube con un `PATCH` que lleva únicamente esos campos, y con
     la respuesta avanzan la réplica y la base;
   - cambiaron los dos al mismo valor: la base avanza a ese valor;
   - cambiaron los dos a valores distintos: conflicto, y ni la réplica ni la base de ese campo
     se tocan hasta que decida la persona.
5. **Una obra de la réplica que no vino en la descarga no se borra.** Se confirma con
   `GET /api/v1/catalog/items/{id}`, y sólo un 404 la marca como "ya no está en tu instancia"
   (caso 9).

Todas las comparaciones son sobre valores normalizados (caso 13), y `status` con `watched_at`
cuentan como un solo campo (caso 5).

## Los casos

### 1. El teléfono edita sin red; la web no toca la obra

- **Debería:** al sincronizar, el cambio llega tal cual al servidor.
- **Hoy:** llega, con dos condiciones.
  - `patch_personal` (`src/movie_inbox/application/catalog_service.py`) aplica sólo los campos
    que trae el `PATCH`, así que subir lo que cambió no pisa lo demás.
  - Un `status: watched` sin `watched_at` conserva la fecha que la obra ya tenía o, si no
    tenía, pone **la del servidor al momento de sincronizar**: `today_date`
    (`domain/catalog.py`) usa la hora local del servidor, que en Docker es UTC. Una obra
    marcada vista el lunes sin red y sincronizada el jueves queda vista el jueves.
- **Hace falta, en el cliente:** mandar sólo lo que cambió. Al marcar vista, guardar la fecha
  local de ese momento y mandarla siempre junto con el `status`.

### 2. La web edita; el teléfono no

- **Debería:** el teléfono toma el valor de la web.
- **Hoy:** converge. La descarga trae el estado personal completo de cada obra
  (`_device_item_payload`, `web/routers/device_catalog.py`), y el teléfono lo compara con su
  base.
- **Hace falta, en el cliente:** que la base de ese campo avance en la misma transacción en
  que la réplica toma el valor del servidor. Si no, aparece la trampa del caso 8.

### 3. Los dos cambian el mismo campo al mismo valor

- **Debería:** converge sin preguntar.
- **Hoy:** converge, si se compara sobre valores normalizados. El servidor guarda igual un
  puntaje 0 y uno vacío, y recorta los espacios de la review (caso 13). Comparar en crudo
  inventaría conflictos entre `null` y `0`, o entre `"hola"` y `"hola "`.
- **Hace falta, en el cliente:** normalizar antes de comparar. No hace falta ningún `PATCH`:
  la base avanza a ese valor.

### 4. Los dos cambian el mismo campo a valores distintos

- **Debería:** decide la persona (ADR-0005, §4).
- **Hoy:** el servidor no interviene. Es una comparación del teléfono contra su base.
- **Hace falta:**
  - **Cliente:** el conflicto se guarda por campo, sin tocar la réplica ni la base de ese
    campo, y la sincronización sigue con todo lo demás. Resolver es elegir un valor: si gana
    el del servidor, la base avanza; si gana el del teléfono, se sube con un `PATCH`.
  - **Owner:** cómo lo ve la persona (decisión 2).
- **Una corrección al plan v3.** Pide que la interfaz de conflictos muestre "de cuándo es cada
  uno". El servidor no guarda cuándo cambió un campo, y ADR-0005 (§6.1) decidió no agregarlo.
  Del lado del servidor sólo se puede decir cuándo se bajó ese valor.

### 5. Cambian campos distintos de la misma obra

- **Debería:** se toman los dos cambios. ADR-0005, §4: cambiar el puntaje en el teléfono y la
  review en la web "converge sin molestar a nadie".
- **Hoy:** converge para `rating` y `review`. **No para `status` con `watched_at`**, porque el
  servidor los ata:
  - `status: to_watch` borra la fecha, aunque el mismo `PATCH` traiga una;
  - la web marca vista con la fecha de hoy si no le dan otra (`update_status`).

  Si el teléfono pasa una obra a pendiente y la web le cambia la fecha, fusionar por separado
  daría "pendiente con la fecha nueva". Y el `PATCH` con `to_watch` borraría esa fecha en el
  servidor: la edición de la web se pierde sin aviso.
- **Hace falta, en el cliente:** tratar `status` y `watched_at` como un solo campo, "visto".
  Si los dos lados lo cambiaron distinto, es un conflicto.

### 6. Dos teléfonos con la misma cuenta, uno después del otro

- **Debería:** cada uno converge con el servidor, y a través de él con el otro.
- **Hoy:** converge.
  - La base es por par teléfono–servidor (ADR-0005, "Qué queda abierto", punto 3).
  - Los ids de obra son los mismos en los dos teléfonos. `_opaque_item_id` los deriva del
    secreto durable de la instancia, del catálogo, de la posición de la fuente y del id de la
    obra, nunca de la sesión.
  - Si el primero sube un puntaje y el segundo no lo tocó, el segundo lo toma. Si el segundo
    también lo cambió, a otro valor, tiene un conflicto.
- **Hace falta:** nada fuera de las reglas generales. Si los dos cambian lo mismo **mientras**
  el otro sincroniza, es el caso 7.

### 7. Una edición en la web, o en otro teléfono, entre que este baja y sube

- **Debería:** la segunda edición no pisa la primera sin que nadie se entere.
- **Hoy:** **la pisa.** `patch_personal` escribe lo que llega sin comparar con nada. El
  teléfono fusionó con lo que bajó, no con lo que hay al subir: si en esos segundos la web
  cambió el mismo campo, gana el `PATCH` y la edición de la web desaparece. Es lo que
  anticipaba [X2], ahora confirmado en el código.
- **Hace falta, en el servidor: [X2]**, con cuatro requisitos que salen de esta matriz:
  1. El `PATCH` recibe, por campo y como opcional, **el valor que el teléfono vio en el
     servidor** al bajar. No la base, que puede ser más vieja.
  2. Si el campo ya vale lo que el teléfono pide, se acepta. Así, reintentar un `PATCH` que se
     aplicó pero cuya respuesta se perdió no falla (caso 8).
  3. Si no coincide, responde 409 con el estado personal actual, para volver a fusionar esa
     obra sin otra consulta.
  4. `status` y `watched_at` se comparan juntos (caso 5).

  El teléfono tiene que saber que el servidor lo acepta antes de mandarlo. `PersonalPatch`
  declara `additionalProperties: false` y `patch_personal` rechaza campos desconocidos, así que
  un servidor sin [X2] respondería 400. La única señal de hoy, `X-Movie-Inbox-Api-Version: 1`
  (`web/app.py`), no distingue capacidades: [X2] tiene que agregar una.
- **Mientras tanto, en el cliente:** un `GET` de la obra justo antes del `PATCH` achica la
  ventana, pero no la cierra.

### 8. Una sincronización cortada a la mitad, y su reintento

Depende de dónde se corta.

- **En la descarga.** Si la réplica y la base se tocan recién con todo bajado (paso 2), el
  teléfono no cambió nada. El reintento empieza de cero, porque un cursor puede dejar de valer
  (caso 15). **Converge.**
- **Entre dos `PATCH`.** Lo que subió ya está en el servidor. En la próxima, servidor y
  teléfono tienen el mismo valor, distinto de la base: "cambiaron los dos al mismo valor".
  **Converge.**
- **Un `PATCH` sin respuesta.** El servidor pudo haberlo aplicado. Reintentar con los mismos
  valores hoy es inocuo, porque escribe lo mismo; con [X2] tiene que seguir siéndolo
  (requisito 2 del caso 7). **Converge.**
- **Una renovación de sesión sin respuesta.** `rotate_device_session`
  (`infrastructure/identity_repository.py`) invalida el refresh token viejo en la misma
  transacción en que crea el nuevo, sin período de gracia. Si la respuesta no llega, el
  teléfono se queda con un token que ya no sirve: la próxima renovación da 401
  `device_session_invalid` y hay que volver a aparear con un QR. No se pierde nada, porque los
  cambios esperan (caso 12), pero un corte de red obliga a ir a la computadora. **Se traba.**
- **Un alta sin respuesta.** El reintento vuelve como `duplicates` y no se duplica
  (`append_device_items`), salvo por el hueco del caso 10. **Converge, con esa excepción.**

**Hace falta:**
- **Servidor:** que la renovación se pueda reintentar, aceptando el refresh token anterior
  durante unos segundos o devolviendo el mismo par si se repite.
- **Owner:** la redacción del invariante 3 (decisión 4), por la trampa que sigue.

**La trampa en la regla de la base.** El invariante 3 de `CLAUDE.md` dice que la base "sólo
avanza cuando una sincronización termina entera". La intención es correcta: un corte no puede
dejar al teléfono creyendo que convergió. Pero leída al pie de la letra, con una réplica que
toma los valores del servidor a medida que fusiona, **pierde datos**:

1. La base dice `to_watch`. La web marca la obra vista.
2. El teléfono baja `watched`, lo pone en la réplica y se corta antes de terminar. La base
   sigue en `to_watch`.
3. La persona vuelve a pasarla a pendiente en el teléfono: la réplica dice `to_watch`.
4. En la próxima sincronización, la réplica es igual a la base y se lee "el teléfono no
   cambió": gana el servidor. **Queda vista, y el cambio de la persona se perdió sin aviso.**

Hay dos formas de cumplir la intención sin la trampa:

- **Todo junto al final:** la réplica tampoco cambia hasta terminar. No pierde nada, pero lo
  que se subió antes del corte vuelve como conflicto si la persona lo edita en el medio.
- **Base por campo, sólo con valores que confirmó el servidor** —la descarga o la respuesta de
  un `PATCH`—, en la misma transacción que la réplica, y nunca con un valor supuesto. No pierde
  nada, y sólo inventa un conflicto si el corte cae justo en la respuesta de un `PATCH`.

Recomiendo la segunda. Es un invariante del repositorio, así que decide el owner.

### 9. Una obra que en el servidor se fusionó con un duplicado o se borró, con cambios pendientes en el teléfono

- **Debería:** el cambio no se pierde en silencio, y el teléfono no borra nada por su cuenta.
  ADR-0005, §3: propagar bajas "exige tombstones y su propio ADR".
- **Hoy:**
  - **Fusión** (`merge`, `application/curation_workflow.py`). Sobrevive una de las dos obras,
    con su id, y la otra desaparece. Los campos personales están protegidos (`MERGE_FIELDS`,
    `domain/merge_review.py`): si difieren, la persona elige en el comparador. La
    sobreviviente puede cambiar de estado personal, y eso para el teléfono es un cambio del
    servidor más (casos 2 y 4).
  - **Borrado** (`delete_item`, `application/catalog_service.py`): sin rastro.
  - **Lo que ve el teléfono:** la obra no viene en la descarga, lo que por sí solo no prueba
    nada (caso 16), y su `GET` y su `PATCH` responden 404 `item_not_found`. No puede saber si
    se fusionó, con cuál, o si se borró.
- **Hace falta:**
  - **Cliente:** tras el 404, la obra queda marcada "ya no está en tu instancia", con sus
    cambios, y no se borra sola. La persona puede darla de alta otra vez como borrador, por el
    camino de importación (caso 10), o quitarla del teléfono.
  - **Owner:** si eso es lo que quiere ver (decisión 5).
  - **Servidor, a futuro y con su ADR:** recordar los ids que existieron y decir qué pasó con
    ellos —por ejemplo, un 410 con el id de la sobreviviente—, para que un cambio pendiente
    siga a la obra fusionada. No hace falta para [A2.2].

### 10. La misma obra dada de alta sin conexión en dos teléfonos, o una que ya existía

- **Debería:** nada entra solo al catálogo, y el teléfono se entera de cómo terminó cada alta.
- **Hoy:**
  - **La recepción está bien.** `append_device_items` (`application/import_service.py`)
    clasifica cada alta contra el catálogo y contra lo que ya tiene el borrador. La misma obra
    desde dos teléfonos, con ids de cliente distintos, llega la segunda vez como `present`
    (`test_adding_the_same_film_twice_on_different_days_is_caught`). Una que ya estaba llega
    `present` o `review`, nunca unida sola. Todo queda para revisar en la web.
  - **El resultado no le llega al teléfono.** La respuesta del `POST` dice el estado al
    recibir, y ningún endpoint dice qué pasó después: aplicada, unida a otra obra o descartada.
    El servidor guarda un resultado por entrada al aplicar el borrador (`result_json`), pero no
    se lo da al teléfono. Cuando la persona aplica el alta en la web, el teléfono baja la obra
    nueva y conserva su alta local: **la ve dos veces**. Unirlas por título sería decidir
    identidad en el teléfono, y eso lo prohíbe el invariante 1.
  - **Un hueco en el reintento.** La idempotencia se busca sólo en el borrador del dispositivo
    que sigue abierto (`_device_draft` exige `status == "ready"`). Si entre el envío y el
    reintento la persona aplicó o borró ese borrador en la web, el reintento crea uno nuevo con
    las mismas obras.
  - **Tres rechazos que el contrato no declara.** 409 `draft_busy`, mientras el borrador se
    aplica en la web; 409 `device_draft_full`, al pasar las 2000 obras; y 409
    `draft_limit_reached`. Para esta ruta el contrato sólo declara 200, 400 y 401.
- **Hace falta:**
  - **Servidor: recibos de altas.** Recordar por cuenta los ids de cliente recibidos, más allá
    de la vida del borrador, y exponer el estado de cada uno: pendiente, aplicada con el id de
    la obra, o descartada. Cierra los dos huecos a la vez.
  - **Cliente:** ante un 409, conservar las altas y reintentar más tarde, con un aviso de que
    hay que revisar en la web.

### 11. Desaparear con cambios pendientes, volver a aparear con la misma cuenta, y aparear con otra

- **Debería:** al desaparear, los datos persisten (decisión del owner del 2026-09-13). Con la
  misma cuenta, los pendientes siguen su camino; con otra, nunca se mezclan.
- **Hoy:**
  - **Desaparear:** `DELETE /api/v1/auth/session` borra la sesión en el servidor. El servidor
    no sabe nada de lo que queda en el teléfono.
  - **La misma cuenta:** los ids de obra no dependen de la sesión (caso 6). Al volver a aparear,
    los pendientes apuntan a las mismas obras. **Converge.**
  - **Otra cuenta, u otra instancia:** los ids no coinciden por construcción, porque cambia el
    catálogo o el secreto, así que el servidor no puede mezclar nada. El riesgo está en el
    teléfono: subir pendientes de una cuenta con la sesión de otra.
  - **Cómo saber qué cuenta es:** el QR trae `instance_id` y `account` (`pairing_payload`,
    `domain/pairing.py`), pero `account` es el **nombre de usuario**, que se puede renombrar
    desde la web (`/api/members/{id}/profile`). El canje devuelve `identity.user.id` e
    `identity.catalog.id` (`identity_payload`, `web/responses.py`), que no cambian.
- **Hace falta:**
  - **Cliente:** "la misma cuenta" es el mismo `instance_id` con el mismo `user.id`, nunca el
    nombre. Pendientes y base guardan la identidad con la que se crearon, y una sesión de otra
    identidad nunca los sube. El `account` del QR sirve para avisar antes de gastar el ticket,
    no para decidir.
  - **Owner:** si se puede editar con el teléfono desapareado (decisión 1), y qué pasa con los
    datos al aparear otra cuenta (decisión 3).

### 12. Sesión revocada o contraseña cambiada, con cambios pendientes

- **Debería:** los cambios esperan, y la persona vuelve a aparear.
- **Hoy:**
  - Cambiar la contraseña o desactivar la cuenta borra las sesiones de dispositivo y los
    tickets de apareamiento. La llamada siguiente da 401 `device_session_invalid`.
  - Si un administrador resetea la contraseña, la cuenta queda con `must_change_password`.
    Hasta que la persona la cambie en la web, un teléfono no puede entrar, aparear ni renovar
    (`password_change_required`).
  - Los ids no cambian (caso 11), así que los pendientes siguen valiendo al volver.
  - **Un vencimiento que conviene tener presente:** la renovación vence a los 30 días y se
    reinicia cada vez que el teléfono renueva la sesión (`DEFAULT_DEVICE_REFRESH_TTL_SECONDS`,
    `application/auth_service.py`). Un teléfono que pasa un mes sin sincronizar tiene que
    volver a aparear, y en una aplicación pensada para usar sin red eso puede pasar.
- **Hace falta:**
  - **Cliente:** un 401 o un 403 nunca descarta pendientes. La réplica queda como estaba, con un
    aviso para volver a aparear.
  - **Owner:** si 30 días está bien para un teléfono (decisión 6).

### 13. Campos que se afectan entre sí

Leído en `patch_personal`, `update_personal` y `update_status`
(`application/catalog_service.py`), `normalize_rating` (`domain/normalization.py`) y
`normalize_date` (`domain/catalog.py`):

| Campo | Qué hace el servidor | Regla del cliente |
| --- | --- | --- |
| `rating` | Guarda `null` y `0` como 0, y sirve 0 como `null`. El contrato admite `minimum: 0`, pero 0 no es un puntaje | 0 es `null` antes de comparar; para borrar, `null` |
| `review` | Recorta los espacios de los extremos, guarda vacío para `null` y `""`, y sirve vacío como `null` | Recortar, y vacío es `null`, antes de comparar y de mostrar |
| `status` y `watched_at` | `to_watch` borra la fecha aunque venga una; `watched` sin fecha conserva la que había o pone la del servidor | Un solo campo, "visto" (caso 5); la fecha local viaja siempre con `watched` (caso 1) |
| `watched_at` solo | Se guarda aunque la obra esté `to_watch`: `patch_personal` sólo la descarta si el mismo `PATCH` trae `to_watch`, y la ficha web la escribe sin mirar el estado | No crear "pendiente con fecha", pero conservarla si viene del servidor |
| Formato de `watched_at` | Corta el texto en sus primeros `AAAA-MM-DD`, y el `PATCH` además valida la fecha | Mandar `AAAA-MM-DD` |
| `status` vacío | Se sirve como `to_watch` | Nada |

Del lado del servidor queda una pregunta menor, sin apuro: si una obra `to_watch` puede tener
fecha. Hoy la admiten la web y la API.

### 14. Una ficha abierta en la web desde antes de sincronizar

- **Debería:** guardar la review en la web no deshace el puntaje que subió el teléfono.
- **Hoy:** **lo deshace.** La ficha manda juntos `watched_at`, `rating` y `review`, con los
  valores que tenía el formulario al abrirse (`/api/personal`, que llama a `update_personal`).
  Si el teléfono subió un puntaje mientras esa ficha estaba abierta, guardar la review lo
  vuelve al valor viejo. Para el teléfono es un cambio normal del servidor, así que lo toma sin
  conflicto. Pasa igual entre dos pestañas del navegador.
- **Hace falta, en el servidor y en el frente visual:** que la ficha mande sólo lo que la
  persona tocó, o que lleve la misma precondición que [X2]. [X2] sola no alcanza: protege el
  `PATCH` del teléfono, no la escritura de la web.

### 15. El servidor se reinicia en medio de la descarga

- **Hoy:** el cursor de página va firmado con el `api_token`, que `serve` genera al azar en
  cada arranque (`web/server.py`). Tras un reinicio, la página siguiente da 400
  `invalid_request`. En Docker, cada backup con `scripts/docker-backup.sh` detiene y vuelve a
  arrancar la aplicación.
- **Hace falta:**
  - **Cliente:** ante un 400 con cursor, empezar la descarga de nuevo. Como no se tocó nada
    (paso 2), no hay nada que deshacer.
  - **Servidor, opcional:** firmar el cursor con el secreto durable, como ya se hace con los
    ids.

### 16. Altas, bajas o cambios de título durante la descarga

- **Hoy:** la lista va ordenada por título y año (`_device_catalog_entries`) y se pagina por
  posición (`_page`). Si entre dos páginas una obra entra, sale o cambia de título, las
  siguientes se corren un lugar: la descarga puede saltear una obra o repetirla. Editar el
  estado personal no mueve nada.
- **Hace falta:**
  - **Cliente:** una obra repetida cuenta una vez. Una ausente no es una baja: se confirma con
    su `GET` (caso 9), o se actualiza en la próxima sincronización.
  - **Servidor, opcional:** paginar por clave —título, año e id— en vez de por posición, lo que
    elimina el corrimiento por altas y bajas.

### 17. Cambia la posición de una fuente del catálogo

- **Hoy:** la posición de la fuente entra en el id de cada obra: `SessionCatalog.from_identity`
  (`web/dependencies.py`) numera las fuentes en orden. Si se agrega, se quita o se reordena una
  fuente, cambian los ids de todas sus obras. Para el teléfono, todo lo que tenía da 404 y todo
  lo que baja es nuevo: duplicaría la réplica y dejaría huérfanos los pendientes. No hay forma
  de pasar de un id viejo al nuevo. Es raro: la instalación con Docker arranca con una sola
  fuente.
- **Hace falta, en el cliente:** si en una descarga completa falta la mayoría de las obras
  conocidas y aparece una cantidad parecida de desconocidas, frenar y avisar en vez de
  procesarlo.

### 18. Dos sincronizaciones a la vez en el mismo teléfono

- **Hoy:** la renovación rota el token (caso 8). Si dos renovaciones compiten, una recibe 401,
  y si esa respuesta se procesa última puede pisar los tokens buenos que guardó la otra.
- **Hace falta, en el cliente:** una sola sincronización a la vez, una sola renovación a la vez,
  y los tokens nuevos guardados antes de usarlos.

## Lo que va a [A5.2]

Reglas del cliente, cada una con su prueba:

1. Una sola sincronización y una sola renovación a la vez (18).
2. Bajar todo antes de tocar la réplica o la base; ante un 400 con cursor, empezar de nuevo
   (8, 15).
3. Una obra repetida en la descarga cuenta una vez; una ausente no es una baja (16).
4. Normalizar antes de comparar: `rating` 0 es `null`, y la review se recorta y vacía es
   `null` (3, 13).
5. `status` y `watched_at` son un solo campo, y la fecha local viaja siempre con `watched`
   (1, 5, 13).
6. Fusión por campo contra la base, con la tabla de ADR-0005, §4.
7. La base de un campo avanza sólo con un valor que confirmó el servidor, en la misma
   transacción que la réplica. Depende de la decisión 4.
8. Un `PATCH` lleva sólo los campos que cambiaron.
9. Un 404 de una obra la marca "ya no está en tu instancia", sin borrarla (9).
10. Un 401 o un 403 nunca descarta pendientes (12).
11. Pendientes y base llevan la identidad `instance_id` y `user.id`, y sólo los sube una sesión
    de esa identidad (11).
12. Frenar ante un cambio masivo de ids (17).

## Lo que el servidor tendría que cambiar: [A5.4]

En orden de importancia para [A2.2]. Los números se asignan al anotarlos en el tablero del
servidor.

| Cambio | Casos | Por qué | Tamaño |
| --- | --- | --- | --- |
| **[X2]**: precondición del `PATCH`, con los cuatro requisitos del caso 7 | 7 | Una edición cruzada se pierde sin aviso | Grande: toca el contrato |
| Renovación de sesión reintentable | 8, 18 | Un corte de red en la renovación obliga a volver a aparear | Medio |
| Recibos de altas: ids de cliente recordados y el estado de cada alta | 10 | El teléfono ve dos veces lo aplicado, y un reintento tardío duplica | Grande: ruta nueva en el contrato |
| Declarar en el contrato los 400, 409 y 503 que ya se responden | 10, 15 | Un cliente fiel al contrato no los espera | Chico |
| Ficha web que escribe sólo lo que cambió, o con precondición | 14 | Una ficha vieja deshace lo que subió el teléfono | Medio; con el frente visual |
| Cursor firmado con el secreto durable | 15 | Un reinicio corta la descarga | Chico, opcional |
| Paginación por clave | 16 | Una alta o una baja durante la descarga saltea o repite obras | Medio, opcional |
| Bajas y fusiones visibles para un teléfono, con su ADR | 9 | Un cambio pendiente sobre una obra fusionada queda huérfano | Grande; después de [A2.2] |

## Decisiones para el owner

1. **¿Se puede editar con el teléfono desapareado?** Recomiendo que sí: los cambios quedan
   pendientes y viajan al volver a aparear **la misma** cuenta. Los ids lo permiten (caso 11).
2. **¿Cómo ve la persona un conflicto?** Recomiendo una lista de conflictos por obra, campo por
   campo, con los dos valores. Del lado del servidor se muestra cuándo se bajó el valor, porque
   no hay registro de cuándo cambió (caso 4). La sincronización termina igual para todo lo
   demás.
3. **¿Qué pasa con los datos al aparear otra cuenta?** Recomiendo no dejar aparear otra cuenta
   mientras haya cambios o altas sin subir. Sin pendientes, avisar y reemplazar la réplica: los
   datos de la cuenta anterior siguen en su instancia. Guardar las dos réplicas contradice "una
   cuenta por instalación" (ADR-0005).
4. **La redacción del invariante 3.** Propongo: "La base de un campo sólo avanza con un valor que
   confirmó el servidor, en la misma transacción que la réplica local; nunca con un valor
   supuesto". La redacción actual, leída al pie de la letra, pierde datos (caso 8).
5. **Una obra que ya no está en la instancia, con cambios pendientes.** Recomiendo mostrarla
   marcada, con sus cambios, y ofrecer darla de alta de nuevo o quitarla del teléfono; nunca
   borrarla sola (caso 9).
6. **El vencimiento de la sesión de un teléfono.** Hoy son 30 días sin renovar. Alargarlo es
   cambiar una constante del servidor, pero una sesión más larga es también un token robado
   que dura más (caso 12).

## Respuestas del owner, 2026-09-14

1. **Editar con el teléfono desapareado: sí**, como se recomendó.
2. **Cómo se ve un conflicto: queda para retomar**, junto con una explicación más clara de la
   fecha. El owner pidió además que el servidor registre cuándo cambia cada campo personal,
   para tener historial y facilitar la sincronización: es **[X3]** del servidor. La marca
   informa a la persona; no decide un conflicto.
3. **Otra cuenta: como se recomendó.** Primero se sincroniza la cuenta actual, y después se
   permite cambiar. Mejorarlo más adelante queda como idea.
4. **La redacción del invariante 3: pendiente**, hasta explicarlo mejor.
5. **Una obra que ya no está: pendiente, y con otro alcance.** El owner quiere que borrar una
   obra en un lado la borre también en el otro al sincronizar. Eso revierte ADR-0005 (§3, "la
   sincronización nunca borra") y el invariante 3 de este repositorio, así que se confirma con
   sus condiciones antes de anotarlo como decisión.
6. **La sesión de un teléfono no vence por tiempo**, y volver a aparear no puede dar problemas.
   Es **[X4]** del servidor, que además hace reintentable la renovación (caso 8) y permite
   revocar teléfonos desde la web.

## Segunda ronda de respuestas, 2026-09-14

Reemplaza lo anotado arriba en los puntos 2, 4, 5 y 6.

2. **Un conflicto de puntaje se resuelve solo, por el más alto**, siempre. Es así por
   definición y se puede cambiar más adelante; mostrar conflictos no es prioridad. Falta
   definir qué pasa con la review y con "visto".
4. **Cambia la redacción del invariante 3**, como se propuso: la base de un campo sólo avanza
   con un valor que confirmó el servidor. Si eso trae otro problema, la alternativa anotada es
   la otra forma del caso 8, descartar la sincronización que quedó a medias y pedir que se
   haga de nuevo, que al owner le parece mejor y más escalable a futuro.
5. **Los borrados viajan**, con las condiciones propuestas:
   - sólo viajan como registro de que una persona borró, nunca porque una obra falte;
   - si un lado borró y el otro editó, decide la persona;
   - unir duplicados cuenta como borrar el duplicado, y lo pendiente pasa a la obra que queda.

   Revierte ADR-0005, §3, y es [X5] del servidor. El owner sugiere seguir cada obra con un
   identificador propio y durable, lo que también resolvería el caso 17.
6. **La llave vence**: pasado un mes hay que volver a escanear el QR, y por seguridad podría
   pedirse más seguido. Queda sin efecto lo anotado en la primera ronda. [X4] conserva la
   renovación reintentable y la revocación desde la web.

## Lo que [A5.3] tiene que reproducir contra un servidor real

Las filas donde esta matriz afirma que algo se pierde o se traba, que son las que justifican
cambiar el servidor:

- el caso 7: dos ediciones cruzadas del mismo campo, y cuál sobrevive;
- el caso 8: una renovación cuya respuesta se descarta, y la siguiente renovación;
- el caso 10: un alta reintentada después de aplicar el borrador en la web;
- el caso 14: una escritura de `/api/personal` con valores viejos después de un `PATCH`;
- el caso 15: una página pedida con un cursor de antes de reiniciar el servidor;
- el caso 16: una alta en la web entre dos páginas de la descarga.
