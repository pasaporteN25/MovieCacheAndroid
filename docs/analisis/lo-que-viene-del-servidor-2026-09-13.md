# Lo que este cliente toma del servidor

**Fecha:** 2026-09-13. **Contexto:** al mudar el cliente a su propio repositorio, qué parte
de lo que ya existe en Movie Inbox hay que reimplementar acá, qué parte se consume por la API
y qué parte no se trae nunca. Las rutas `src/...` y `docs/...` de este documento son del
repositorio del servidor; `contract/` es de este.

## El criterio

No todo lo que sabe hacer el servidor lo tiene que saber hacer el teléfono. Hay cuatro casos:

| Caso | Qué quiere decir | Ejemplo |
| --- | --- | --- |
| **Se consume** | El servidor lo resuelve y el resultado viaja por la API | Identidad, dificultad de charadas, disponibilidad |
| **Se porta con vectores** | Tiene que dar exactamente lo mismo que el servidor, o se rompe algo | El generador de charadas: si no coincide, dos teléfonos juegan con mazos distintos |
| **Se porta sin paridad exacta** | Conviene que se parezca, y se mide contra los mismos casos | La búsqueda |
| **No se trae** | No tiene sentido en un teléfono, o una regla lo prohíbe | Scanner, rutas, curaduría |

**Vectores**: casos de prueba con entrada y salida calculadas por el propio servidor, que el
servidor recalcula en cada corrida de sus pruebas. El port en Kotlin tiene que pasar los
mismos casos. Si el servidor cambia algo que alteraría la salida, falla su propia suite
antes de que un teléfono instalado se entere.

## Se porta con vectores

### Leer el QR y confiar en el certificado — [A3.1], para [A2.1]

- **En el servidor**: `src/movie_inbox/domain/pairing.py`. `pairing_payload` arma lo que lleva
  el QR —`v: 1`, `origin`, `ticket`, `expires_at`, `instance_id`, `account` y, cuando la
  instancia usa certificado propio, `certificate_pin`—, y `certificate_pin_from_pem` calcula
  el pin: SHA-256 del `SubjectPublicKeyInfo`, en base64.
- **Qué hace el teléfono**: validar el payload y, si trae pin, construir un trust anchor que
  acepte sólo el certificado cuya huella coincide.
- **Vectores**: `tests/test_pairing_certificate.py` ya tiene dos certificados reales —uno
  P-256 y uno RSA-2048, elegidos porque uno cabe en una longitud DER corta y el otro no, que
  es donde falla un parser escrito a mano— con el pin que imprimió OpenSSL. Falta llevarlos a
  un JSON: tarea [X1.1] del servidor.

### Normalización de títulos — [A3.2], para [A2.3]

- **En el servidor**: `normalize_search_text` (`domain/normalization.py`), y `title_match_key`,
  `title_similarity` y `FUNCTION_WORDS` (`domain/catalog.py`).
- **Qué hace el teléfono**: al dar de alta sin red, avisar si el título se parece a algo que ya
  está en la réplica. Es un aviso: la identidad la decide el servidor, con revisión humana.
- **Dónde se rompe un port**: en lo que Python hace solo. `normalize_search_text` desescapa
  HTML, normaliza con NFKC y quita diacríticos **sólo** de letras latinas: "Adiós" encuentra
  "Adios", pero el japonés conserva sus caracteres. `title_match_key` quita paréntesis y
  años sueltos, salvo el primero, porque "1917" y "2001" también son títulos. Y
  `title_match_keys_for_item` mira además archivos locales, que el teléfono no tiene: esa
  parte no se porta.
- **Vectores**: no existen todavía. Tarea [X1.2] del servidor.

### Generador de charadas — [A3.3], para [A2.4]

- **En el servidor**: `src/movie_inbox/domain/charades.py`: FNV-1a de 32 bits, huella de seis
  caracteres, semilla y barajado Fisher-Yates con un LCG documentado.
- **Vectores**: **listos**, en `contract/charades-v1-vectors.json`. Incluyen una trampa a
  propósito: claves cuyo orden por punto de código no coincide con el orden por unidad
  UTF-16, que es el que usa `String.compareTo` en Kotlin.

## Se porta sin paridad exacta

### Búsqueda sin conexión — [A3.4]

- **En el servidor**: `search_catalog_items` (`application/search_service.py`), sobre
  `domain/search.py`: el ranking que ajustó [B1] con once arreglos medidos.
- **Qué hace el teléfono**: buscar en su réplica sin red.
- **Por qué sin paridad exacta**: el ranking va a seguir cambiando en el servidor, y exigir
  que el teléfono coincida punto por punto ataría cada ajuste a una versión nueva de la app.
  Lo que sí se exige es pasar los mismos casos.
- **Casos**: el corpus dorado del servidor, `src/movie_inbox/search_lab/corpus/v1.json`, con
  32 casos que marcan resultados relevantes y prohibidos. Se copia a `contract/` cuando
  arranque la tarea, no antes, porque cambia seguido.

### Reglas simples que se replican

| Regla | En el servidor | Para |
| --- | --- | --- |
| Tipos de obra: `pelicula`, `serie`, `anime`, `documental` | `VALID_KINDS`, `domain/normalization.py` | [A2.1] |
| Estado personal: `to_watch` o `watched`; puntaje de 1 a 10, o vacío | `VALID_STATUSES` y `normalize_rating`, `domain/normalization.py` | [A2.2] |
| Qué título se muestra primero | `COLLECTION_DISPLAY_TITLE_FIELDS` (`domain/collections.py`); en charadas, `playable_title` (`domain/charades.py`) | [A2.1], [A2.4] |
| Borrador de alta: id del cliente, idempotente, no vence | `DEVICE_ORIGIN` y `NEVER_EXPIRES` (`domain/imports.py`); esquema `OfflineDraftItem` del contrato | [A2.3] |

## Se consume por la API y no se porta

| Qué | Por qué no en el teléfono |
| --- | --- |
| Identidad y enriquecimiento (`decide_match`, fuentes externas) | El teléfono nunca decide identidad. Además necesita red y las credenciales de la instancia. |
| Dificultad de charadas | Sale del índice IMDb de ~1,1 GB y viaja resuelta. |
| Disponibilidad en streaming y puntajes públicos | Dependen de TMDb con el token de la instancia. El teléfono sólo los **muestra**, con las reglas de [A2.6]. |
| Qué colecciones sigue la cuenta | Se decide en la web; el teléfono lee las seguidas. |

## No se trae nunca

- **La capa operativa**: rutas, archivos locales, bibliotecas, Scanner, curaduría e
  importaciones web. Un teléfono no tiene tus discos, y la regla de privacidad del servidor
  no se afloja por estar en otro repositorio.
- **Administración**, el **Club** como directorio de la instancia y las **presentaciones
  públicas**.

## Ideas del servidor que podrían tener versión móvil más adelante

No son tareas: son ideas ya probadas en la web que no entran en [A2].

- **La programación de Inicio** (`application/home_service.py`): hasta cuatro
  recomendaciones diarias estables, con selector Hoy / Ayer, calculadas sólo con datos
  locales. Funciona sin red por construcción, así que encaja con un teléfono autónomo.
- **El sorteo al azar**, entre todo el catálogo o sólo entre las obras disponibles.

## Lo que el servidor tiene que hacer por este cliente

Anotado en el tablero del servidor:

- **[X1.1]** exportar los vectores del pin SPKI.
- **[X1.2]** exportar vectores de normalización de títulos.
- **La pantalla que genera el QR**, traspaso al frente visual del servidor. Sin ella no se
  puede probar [A2.1] de punta a punta.
