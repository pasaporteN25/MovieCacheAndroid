# Emparejamiento y primera réplica Android

Estado: roadmap preparado el 2026-10-04; implementación pendiente.
Owner: Lucas. Plan técnico base: `android-client-v3.md`. Norte: `daily-discovery-v1.md`.

## Primera entrega usable

Desde su cuenta web, una persona genera un QR, lo escanea con Android, confirma
la instancia y la cuenta, descarga el catálogo y puede consultarlo sin conexión.
Cerrar y volver a abrir la app conserva el emparejamiento y la réplica. La web
permite reconocer y revocar el teléfono. Ninguna descarga escribe estado personal.

La captura de recomendaciones y la edición bidireccional son los incrementos
siguientes. Una descarga correcta no demuestra todavía que la sincronización
bidireccional preserve cambios: esa garantía tiene pruebas propias en A5.

## Arquitectura acordada

- Compose para pantallas nuevas, MVVM con `ViewModel` y estado observable inmutable.
  Compose no exige abandonar MVVM. La pantalla renderiza estado y emite acciones;
  no accede directamente a red, tokens ni base de datos.
- `StateFlow` para estado, colección ligada al ciclo de vida y efectos puntuales
  separados. Estados explícitos: inicial, validando, conectando, descargando,
  listo y error recuperable. La rotación no repite el canje de un ticket.
- Repositorios como límite de errores. Room para réplica y cambios pendientes;
  Retrofit/OkHttp para API v1, serialización Kotlin y Hilt/KSP según el plan base.
- Si una integración necesita Views/XML, usar View Binding; evitar acceso manual
  por `findViewById` y Kotlin synthetics. Esto no obliga a usar Data Binding.
- WebView se admite sólo para un contenido web concreto que lo justifique.
  El emparejamiento, la réplica y el sync se implementan nativamente; una WebView
  no reemplaza su contrato ni comparte las cookies de la cuenta con la API nativa.
- Empezar en `:app` con paquetes por función. Extraer las reglas puras de fusión
  a Kotlin sin Android en A5.2; no crear módulos vacíos ni otro patrón de UI.

## Orden de entrega y gates

| Paso | Entrega observable | Tareas existentes | Gate para avanzar |
| --- | --- | --- | --- |
| P0 | Entorno reproducible y contrato actualizado | A2.0, A4.2 | Compilar APK; fijar commit del servidor y copiar OpenAPI/vectores sin editarlos |
| P1 | Primer ciclo de sync probado sin interfaz | A5.2, A5.3 | Descargar, editar, subir y releer con cliente Kotlin contra servidor descartable; reintento y concurrencia sin pérdida silenciosa |
| P2 | Cuenta web muestra QR y sus teléfonos | Traspasos web de A2.1 y X4 | Owner y miembro sólo operan su cuenta; expiración, nueva generación, error de HTTPS y revocación visibles |
| P3 | Android canjea el QR y conserva una sesión segura | A3.1, A2.1 | Payload validado contra vectores, TLS correcto, ticket de un solo uso y credenciales protegidas |
| P4 | Catálogo local consultable sin red | A2.1 | Paginación completa, réplica consistente, reinicio/offline correctos y ausencia de datos operativos |
| P5 | Estado personal se edita y sincroniza | A2.2, A5 | Base confirmada por campo; reglas acordadas y cortes de conexión probados; borrado contra edición llega a la persona |
| P6 | Recomendación guardada desde texto o Compartir | A2.3, A3.2 y brief cotidiano | Borrador durable e idempotente; identificar no se decide por parecido; revisión en servidor |

P2 puede prepararse mientras se prueba P1, porque usa endpoints existentes.
La construcción de pantallas Android de P3/P4 empieza al superar el gate inicial
de P1, conservando la decisión previa de probar sync antes de construir pantallas.
La matriz completa de A5 sigue siendo requisito para entregar edición P5.
v0.1.0 cierra P3/P4 (M2): aparear y leer. No promete edición/captura antes de sus gates.

## Pruebas sin QR ni teléfono

El arnés obtiene una sesión de dispositivo mediante `POST /api/v1/auth/login`
con una cuenta sintética de una instancia descartable. No usa la sesión del browser
ni el `/api/` histórico. Este login es un recurso de prueba, no el onboarding final.
El cliente Kotlin consume la API real; probar sólo un cliente Python no valida el
cliente Android. Las reglas puras usan vectores y no necesitan emulador.

Primer caso: leer obra/base, cambiar un campo, enviar con precondición, releer
respuesta y verificar base/local confirmadas. Después: dos clientes, edición web
entre lectura/escritura, respuesta perdida, renovación reintentada, paginación
interrumpida, bajas y sesión revocada. Conservar los 18 casos de la matriz A5.1.

## Pruebas con el dispositivo

Decisión del owner, 2026-10-04: primera prueba en la misma Wi-Fi que el servidor;
el acceso fuera de casa se valida en un incremento posterior.

1. Conectar por USB con depuración autorizada; comprobar `adb devices` e instalar
   el APK debug. Hoy la pantalla existente sólo comprueba el entorno.
2. Para la primera prueba real, acordar dirección alcanzable desde el teléfono
   y TLS. `127.0.0.1` en el teléfono apunta al teléfono; `10.0.2.2` corresponde al
   emulador y su excepción HTTP de debug no habilita HTTP en un teléfono real.
3. Escanear, mostrar instancia/cuenta y confirmar; canjear sólo entonces.
4. Leer toda la réplica, activar modo avión, reiniciar y consultar una ficha.
5. Probar cámara denegada, QR inválido/vencido/usado, certificado incorrecto,
   caída durante descarga y revocación desde web. El error explica cómo retomar.

TLS sigue CLAUDE.md: certificado válido o trust anchor construido desde SPKI del QR,
con verificación de hostname. No adoptar un certificado por un handshake fallido.
Tokens bajo protección de Android Keystore, fuera de logs/URI/backup. El escáner
es una pantalla nativa; ZXing puede necesitar una integración con Views.

## Dump web: prueba manual aislada

La réplica Android se descarga mediante la API; no importará directamente SQLite
de la web. El dump sirve como semilla de una instancia de QA separada.

- Identificar qué se exportó: catálogo, `instance.db` o exportación portable JSON,
  y versión/commit web. `instance.db` por sí sola no representa todos los catálogos.
- Usar el mecanismo de backup/exportación del servidor. No copiar a ciegas una
  SQLite abierta: su WAL puede contener cambios que la copia omita.
- Mantener la copia fuera del repo y de fixtures. No usar credenciales reales;
  preferir cuenta de QA y exportación de catálogo antes que copiar sesiones.
- Separar rutas de datos, puerto y configuración de producción. Para el primer
  QA de lectura no ejecutar scanner, enriquecimiento ni sincronización de escritura.
- Las pruebas automatizadas siguen con datos sintéticos. El dump privado sólo
  evalúa volumen, contenido y usabilidad durante QA manual autorizado.

No hace falta esperar el dump para P0/P1. El teléfono tampoco bloquea esas tareas.

## Estado comprobado y próxima tarea

2026-10-04: `assembleDebug --offline` exitoso, APK debug existente; `adb devices`
sin dispositivo conectado. Compose placeholder; no pairing, Room ni cliente sync.
Contrato Android fijado en septiembre y pendiente de actualización explícita.
X1–X10 del servidor están implementados; X11 está en release/0.11.0. Verificar
el commit elegido al comenzar P0, sin suponer que la release ya se fusionó.
Los endpoints web de QR/sesiones existen; falta su interfaz de usuario.

Siguiente incremento: P0 + primer caso de P1, con evidencia Kotlin/servidor real.
La red inicial ya está acordada: misma Wi-Fi. Antes del QA físico confirmar
modelo/versión Android y forma de ejecución web.
El diseño visual de pantallas se revisará por separado antes de implementarlo.
