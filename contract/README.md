# Contrato con el servidor

Copias fijas de lo que el servidor de Movie Inbox le promete a este cliente. **No se editan
acá.** Cuando el servidor cambia, se vuelven a copiar a propósito y se actualiza esta tabla.

| Archivo | Qué es | Origen en el servidor | Commit de origen |
| --- | --- | --- | --- |
| `device-api-v1.openapi.json` | La API de dispositivo `/api/v1/`: sesiones, apareamiento, catálogo, edición personal, altas sin conexión, colecciones, disponibilidad, puntajes y charadas | `docs/openapi/device-api-v1.openapi.json` | `5cbc34b`, 2026-09-12 |
| `charades-v1-vectors.json` | Vectores del generador de charadas: FNV-1a, huella, semilla y mazo | `docs/briefs/charades-v1-vectors.json` | `5cbc34b`, 2026-09-12 |

## Por qué una copia, si la app funciona sola

La app es independiente en el sentido que importa: **funciona sin el servidor**. Pero cuando
se sincroniza, los dos lados tienen que entenderse, y eso no se improvisa de un lado solo:

- **El contrato de la API** dice qué trae una obra, qué devuelve el apareamiento y qué acepta
  un alta. Si el teléfono lo supone en vez de leerlo, el primer cambio del servidor se
  descubre en la mano de alguien, con una sincronización que falla.
- **Los vectores de charadas** son el caso extremo: dos teléfonos juegan con el mismo mazo
  sólo si su generador coincide byte a byte con el del servidor. No alcanza con que se
  parezca.

Una copia fija, y no una referencia al otro repositorio, por dos razones. Este repositorio
compila y se prueba solo, sin el servidor al lado. Y un cambio del contrato se vuelve
**visible**: actualizar la copia es un commit con motivo, no una sorpresa en tiempo de
ejecución.

## Cómo actualizar

1. Copiar el archivo desde el servidor, sin editarlo.
2. Anotar en la tabla el commit de origen y la fecha.
3. Correr las pruebas del cliente que lo usan. La que compara el modelo de datos con el
   contrato es [A4.2]; la del generador de charadas, [A3.3].
