# CLAUDE.md

Cliente Android de Movie Inbox: Kotlin + Jetpack Compose, un solo módulo `:app` por ahora.
El servidor es otro repositorio, al lado: `../tengo-una-lista-de-peliculas-en`.

Antes de trabajar, leé `tareas.md`, `docs/roadmap.md` y el plan vigente,
`docs/briefs/android-client-v3.md`. No son documentación decorativa.

## Comandos

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

`JAVA_HOME` hace falta porque el `java` del `PATH` de la máquina de trabajo es 1.8. Si una
descarga de dependencias falla con un error de SSL, es la inspección de HTTPS del antivirus:
ver `docs/briefs/android-setup.md`.

## Invariantes

Si una tarea parece exigir romper alguna, parar y preguntar en vez de decidir solo.

1. **El teléfono nunca decide identidad.** Puede avisar que un alta se parece a algo que ya
   está; la decisión la toma el servidor, con revisión humana.
2. **Nunca la capa operativa.** Rutas, archivos, bibliotecas, Scanner y curaduría no se
   guardan ni se muestran: un teléfono no tiene tus discos. `en_catalogo` se lee, no se
   afirma.
3. **La sincronización la inicia una persona y nunca borra.** Conflictos por campo, con
   fusión a tres bandas contra la base de la última sincronización completa. La base sólo
   avanza cuando una sincronización termina entera.
4. **Secretos.** Tokens sólo en Android Keystore: nunca en preferencias sin cifrar, logs,
   URI, portapapeles ni backups (`allowBackup` sigue en `false`).
5. **TLS.** Certificado válido, o un trust anchor construido desde la huella SPKI que trae el
   QR. Nunca un trust manager permisivo ni sin verificación de hostname.
6. **El contrato manda.** `contract/` es la copia fijada de lo que el servidor promete: no se
   edita a mano. Si el servidor cambia, se vuelve a copiar a propósito y se anota el commit
   de origen.
7. **Lo que tiene que coincidir con el servidor se prueba contra sus vectores**, no contra
   una reinterpretación propia.

## Tablero

`tareas.md` sigue las reglas del tablero del servidor: al cerrar una tarea se mueve a
`Hecho` con fecha y commit, no se borra. Lo que este cliente le pide al servidor se anota en
el tablero del servidor y acá se cita con su número de allá.

El cliente lo escribe Claude, por decisión del owner del 2026-09-07.
