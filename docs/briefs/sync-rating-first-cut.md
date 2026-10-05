# Primer corte de sincronización — puntaje

Entregado el 2026-10-05: P0 y el caso inicial de P1. No cierra A5.2/A5.3 completas.

## Escenario y contrato

Una réplica conoce la base de un puntaje, se edita localmente y una sincronización
manual relee el servidor. La fusión compara base/local/servidor: si sólo cambió
un lado lo toma, si convergen conserva el valor y ante cambios distintos toma
el mayor (null representa ausencia, no cero). Una limpieza unilateral se conserva.

El PATCH incluye sólo rating y base.rating, nunca changed_at ni campos ajenos.
409 personal_conflict obliga a releer y fusionar: máximo tres intentos por ciclo.
El servidor confirma el valor mediante GET o respuesta PATCH. El almacén confirma
base/local conjuntamente y preserva una edición local que ocurrió durante el envío.
Una respuesta perdida no avanza la base; el próximo GET permite converger.

## Implementación y pruebas

- Reglas puras Kotlin, interfaz de réplica y repositorio serializado por Mutex.
- Adaptador Retrofit/OkHttp, serialización Kotlin tolerante a campos adicionales,
  versión de API verificada, bearer por interceptor y redirecciones deshabilitadas.
- TLS de plataforma; HTTP sólo opt-in a 127.0.0.1 para arnés JVM. No se habilitó
  cleartext en Android, cámara, pairing ni persistencia de tokens.
- Réplica en memoria exclusivamente en tests: **no hay persistencia Room todavía**.
- 12 pruebas: cinco de fusión, cuatro de ciclo/recuperación, dos de transporte,
  una de integración que prueba múltiples pasos contra FastAPI/Uvicorn reales.
- Integración: login sintético, descarga, edición 7, PATCH/relectura, cambio 9 en
  otro cliente, rechazo de base vieja, convergencia al mayor y respuesta PATCH
  descartada después de escribir 10, seguida de relectura/reintento seguro.

## Reproducir en Windows

Desde este repo, con la venv del servidor y JDK de Android Studio:

```powershell
$env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
../tengo-una-lista-de-peliculas-en/.venv/Scripts/python.exe scripts/test_sync.py --server ../tengo-una-lista-de-peliculas-en
```

Requiere dependencias Gradle descargadas (el arnés usa --offline). Crea una instancia
sintética en carpeta temporal y puerto efímero loopback; la destruye al terminar.
Verifica SHA del servidor y hashes fijados en contract/source.json. No usa dump,
catálogo privado, sesión real ni teléfono. El arnés falla si se omite la integración.
`testDebugUnitTest` sin arnés ejecuta unitarias y omite la integración explícitamente.

## Gate y siguiente entrega

Build debug y caso inicial P1 verificados; se puede comenzar el frente QR/dispositivos
web y especificar las pantallas de conexión/catálogo. Antes de entregar edición:
completar status/fecha, reviews, bajas contra edición, renovación, paginación,
recibos y demás casos de la matriz A5.1. Antes de catálogo usable: pairing seguro,
Room transaccional, descarga paginada y QA offline/reinicio en teléfono.

La Activity aún muestra el placeholder: este corte es técnico y no publica v0.1.0.
