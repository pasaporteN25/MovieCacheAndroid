# movieIndexAndroid

Cliente Android de **Movie Inbox**, el gestor self-hosted de catÃ¡logo audiovisual. Kotlin y
Jetpack Compose.

**Estado, 2026-09-13:** el proyecto existe y compila, pero todavÃ­a muestra sÃ³lo una pantalla
de prueba. Lo que viene estÃ¡ en [`tareas.md`](tareas.md) y el orden, en
[`docs/roadmap.md`](docs/roadmap.md).

## QuÃ© va a ser

**Norte actualizado, 2026-10-04:** capturar recomendaciones y consultar obras,
directores y actores en el dÃ­a a dÃ­a, con capacidades coherentes con la web y
sincronizaciÃ³n confiable. EspecificaciÃ³n y preguntas abiertas:
[`daily-discovery-v1.md`](docs/briefs/daily-discovery-v1.md). MÃ©todo de entrega: `SDD.md`.

Una aplicaciÃ³n que **funciona sola** y se **sincroniza con tu instancia** de Movie Inbox
cuando vos lo pedÃ­s:

- Se **aparea una vez**, escaneando un QR desde la web, con una cuenta que ya existe en la
  instancia.
- Desde ahÃ­ tenÃ©s **tu catÃ¡logo en el telÃ©fono**: explorarlo, buscar, marcar vistas,
  puntuar y escribir reviews, sin red.
- PodÃ©s **dar de alta pelÃ­culas** en cualquier lado. Es el caso que manda: *guardar una
  pelÃ­cula sin estar frente a la computadora ni en casa*. Viajan al servidor en la prÃ³xima
  sincronizaciÃ³n y pasan por la misma revisiÃ³n que cualquier importaciÃ³n.
- **Charadas** con el mismo mazo en varios telÃ©fonos, sin red.

La sincronizaciÃ³n la inicia una persona. Lo que borrÃ¡s en un lado se borra en el otro, pero
una obra que simplemente falta **nunca borra nada**. Cuando los dos lados cambiaron lo mismo
de forma distinta, se resuelve solo: gana el puntaje mÃ¡s alto, se conservan las dos reviews y
"vista" gana con la fecha mÃ¡s reciente. SÃ³lo un borrado contra una ediciÃ³n **lo decide la
persona**.

## CÃ³mo se relaciona con el servidor

Son dos repositorios a propÃ³sito, por una decisiÃ³n del 2026-08-17: toolchain, CI y ciclo de
versiones distintos para dos cosas que sÃ³lo se hablan por HTTP.

- **El servidor** vive al lado, en `../tengo-una-lista-de-peliculas-en` (GitHub:
  `pasaporteN25/MovieCache`). AhÃ­ estÃ¡n la API de dispositivo `/api/v1/`, el apareamiento y
  las decisiones de arquitectura que rigen este cliente (ADR-0003 y ADR-0005).
- **Este repositorio** consume esa API. Lo que necesita saber de ella â€”el contrato OpenAPI y
  los vectores del generador de charadasâ€” estÃ¡ copiado en [`contract/`](contract/), con el
  commit del servidor del que saliÃ³. Por quÃ© una copia y no una referencia, en
  [`contract/README.md`](contract/README.md).

## Primera entrega mobile

Roadmap de emparejamiento y QA: [`mobile-pairing-v1.md`](docs/briefs/mobile-pairing-v1.md).
Compose + MVVM; View Binding cuando una integraciÃ³n necesita XML/Views.
Verificado el 2026-10-04: build debug exitoso. La app sigue siendo un placeholder;
no implementa todavÃ­a pairing ni sync. Reglas y arnÃ©s se prueban con datos
sintÃ©ticos antes del recorrido QR â†’ rÃ©plica local â†’ consulta offline.

## Compilar

AGP queda fijado en **9.1.1**, compatible con el Android Studio instalado del owner
(decisiÃ³n 2026-10-04). No subirlo sÃ³lo porque la terminal permite compilar: tambiÃ©n
debe admitirlo el IDE. Las versiones viven en `gradle/libs.versions.toml`.

Requiere Android Studio, que trae el JDK y el SDK. Para abrirlo: *File â†’ Open* y elegir esta
carpeta.

Desde una terminal, con el JDK de Android Studio:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

El APK queda en `app\build\outputs\apk\debug\app-debug.apk`. QuÃ© hacer si falla la descarga
de dependencias, en [`docs/briefs/android-setup.md`](docs/briefs/android-setup.md).

## DocumentaciÃ³n

| Archivo | QuÃ© es |
| --- | --- |
| [`tareas.md`](tareas.md) | Tablero: backlog, en curso y hecho |
| [`docs/roadmap.md`](docs/roadmap.md) | Hitos, orden y decisiones que rigen |
| [`docs/analisis/lo-que-viene-del-servidor-2026-09-13.md`](docs/analisis/lo-que-viene-del-servidor-2026-09-13.md) | QuÃ© se reimplementa del servidor, quÃ© se consume y quÃ© no se trae |
| [`docs/analisis/matriz-de-sincronizacion-2026-09-14.md`](docs/analisis/matriz-de-sincronizacion-2026-09-14.md) | QuÃ© pasa en cada caso de sincronizaciÃ³n, quÃ© hace hoy el servidor y quÃ© hay que cambiar |
| [`docs/briefs/android-client-v3.md`](docs/briefs/android-client-v3.md) | Plan de construcciÃ³n vigente |
| [`contract/`](contract/) | Contrato de la API y vectores, copiados del servidor |
| [`CLAUDE.md`](CLAUDE.md) | Reglas del repositorio para agentes |

## Licencia

GPL-3.0, la misma que el servidor. Ver [`LICENSE`](LICENSE).

## Primer corte sync (2026-10-05)

Contrato actualizado y ciclo Kotlin/servidor real del puntaje probado.
[Alcance, límites y reproducción](docs/briefs/sync-rating-first-cut.md).
La interfaz sigue siendo el placeholder; pairing y Room son el próximo incremento.
