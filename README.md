# movieIndexAndroid

Cliente Android de **Movie Inbox**, el gestor self-hosted de catálogo audiovisual. Kotlin y
Jetpack Compose.

**Estado, 2026-09-13:** el proyecto existe y compila, pero todavía muestra sólo una pantalla
de prueba. Lo que viene está en [`tareas.md`](tareas.md) y el orden, en
[`docs/roadmap.md`](docs/roadmap.md).

## Qué va a ser

Una aplicación que **funciona sola** y se **sincroniza con tu instancia** de Movie Inbox
cuando vos lo pedís:

- Se **aparea una vez**, escaneando un QR desde la web, con una cuenta que ya existe en la
  instancia.
- Desde ahí tenés **tu catálogo en el teléfono**: explorarlo, buscar, marcar vistas,
  puntuar y escribir reviews, sin red.
- Podés **dar de alta películas** en cualquier lado. Es el caso que manda: *guardar una
  película sin estar frente a la computadora ni en casa*. Viajan al servidor en la próxima
  sincronización y pasan por la misma revisión que cualquier importación.
- **Charadas** con el mismo mazo en varios teléfonos, sin red.

La sincronización la inicia una persona. Lo que borrás en un lado se borra en el otro, pero
una obra que simplemente falta **nunca borra nada**. Cuando los dos lados cambiaron lo mismo
de forma distinta, el puntaje se resuelve solo, por el más alto, y en lo demás **decide la
persona**.

## Cómo se relaciona con el servidor

Son dos repositorios a propósito, por una decisión del 2026-08-17: toolchain, CI y ciclo de
versiones distintos para dos cosas que sólo se hablan por HTTP.

- **El servidor** vive al lado, en `../tengo-una-lista-de-peliculas-en` (GitHub:
  `pasaporteN25/MovieCache`). Ahí están la API de dispositivo `/api/v1/`, el apareamiento y
  las decisiones de arquitectura que rigen este cliente (ADR-0003 y ADR-0005).
- **Este repositorio** consume esa API. Lo que necesita saber de ella —el contrato OpenAPI y
  los vectores del generador de charadas— está copiado en [`contract/`](contract/), con el
  commit del servidor del que salió. Por qué una copia y no una referencia, en
  [`contract/README.md`](contract/README.md).

## Compilar

Requiere Android Studio, que trae el JDK y el SDK. Para abrirlo: *File → Open* y elegir esta
carpeta.

Desde una terminal, con el JDK de Android Studio:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

El APK queda en `app\build\outputs\apk\debug\app-debug.apk`. Qué hacer si falla la descarga
de dependencias, en [`docs/briefs/android-setup.md`](docs/briefs/android-setup.md).

## Documentación

| Archivo | Qué es |
| --- | --- |
| [`tareas.md`](tareas.md) | Tablero: backlog, en curso y hecho |
| [`docs/roadmap.md`](docs/roadmap.md) | Hitos, orden y decisiones que rigen |
| [`docs/analisis/lo-que-viene-del-servidor-2026-09-13.md`](docs/analisis/lo-que-viene-del-servidor-2026-09-13.md) | Qué se reimplementa del servidor, qué se consume y qué no se trae |
| [`docs/analisis/matriz-de-sincronizacion-2026-09-14.md`](docs/analisis/matriz-de-sincronizacion-2026-09-14.md) | Qué pasa en cada caso de sincronización, qué hace hoy el servidor y qué hay que cambiar |
| [`docs/briefs/android-client-v3.md`](docs/briefs/android-client-v3.md) | Plan de construcción vigente |
| [`contract/`](contract/) | Contrato de la API y vectores, copiados del servidor |
| [`CLAUDE.md`](CLAUDE.md) | Reglas del repositorio para agentes |

## Licencia

GPL-3.0, la misma que el servidor. Ver [`LICENSE`](LICENSE).
