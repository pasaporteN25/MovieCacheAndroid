# Emparejamiento y primera rÃ©plica Android

Estado: roadmap preparado el 2026-10-04; implementaciÃ³n pendiente.
Owner: Lucas. Plan tÃ©cnico base: `android-client-v3.md`. Norte: `daily-discovery-v1.md`.

## Primera entrega usable

Desde su cuenta web, una persona genera un QR, lo escanea con Android, confirma
la instancia y la cuenta, descarga el catÃ¡logo y puede consultarlo sin conexiÃ³n.
Cerrar y volver a abrir la app conserva el emparejamiento y la rÃ©plica. La web
permite reconocer y revocar el telÃ©fono. Ninguna descarga escribe estado personal.

La captura de recomendaciones y la ediciÃ³n bidireccional son los incrementos
siguientes. Una descarga correcta no demuestra todavÃ­a que la sincronizaciÃ³n
bidireccional preserve cambios: esa garantÃ­a tiene pruebas propias en A5.

## Arquitectura acordada

- Compose para pantallas nuevas, MVVM con `ViewModel` y estado observable inmutable.
  Compose no exige abandonar MVVM. La pantalla renderiza estado y emite acciones;
  no accede directamente a red, tokens ni base de datos.
- `StateFlow` para estado, colecciÃ³n ligada al ciclo de vida y efectos puntuales
  separados. Estados explÃ­citos: inicial, validando, conectando, descargando,
  listo y error recuperable. La rotaciÃ³n no repite el canje de un ticket.
- Repositorios como lÃ­mite de errores. Room para rÃ©plica y cambios pendientes;
  Retrofit/OkHttp para API v1, serializaciÃ³n Kotlin y Hilt/KSP segÃºn el plan base.
- Si una integraciÃ³n necesita Views/XML, usar View Binding; evitar acceso manual
  por `findViewById` y Kotlin synthetics. Esto no obliga a usar Data Binding.
- WebView se admite sÃ³lo para un contenido web concreto que lo justifique.
  El emparejamiento, la rÃ©plica y el sync se implementan nativamente; una WebView
  no reemplaza su contrato ni comparte las cookies de la cuenta con la API nativa.
- Empezar en `:app` con paquetes por funciÃ³n. Extraer las reglas puras de fusiÃ³n
  a Kotlin sin Android en A5.2; no crear mÃ³dulos vacÃ­os ni otro patrÃ³n de UI.

## Orden de entrega y gates

| Paso | Entrega observable | Tareas existentes | Gate para avanzar |
| --- | --- | --- | --- |
| P0 | Entorno reproducible y contrato actualizado | A2.0, A4.2 | Compilar APK; fijar commit del servidor y copiar OpenAPI/vectores sin editarlos |
| P1 | Primer ciclo de sync probado sin interfaz | A5.2, A5.3 | Descargar, editar, subir y releer con cliente Kotlin contra servidor descartable; reintento y concurrencia sin pÃ©rdida silenciosa |
| P2 | Cuenta web muestra QR y sus telÃ©fonos | Traspasos web de A2.1 y X4 | Owner y miembro sÃ³lo operan su cuenta; expiraciÃ³n, nueva generaciÃ³n, error de HTTPS y revocaciÃ³n visibles |
| P3 | Android canjea el QR y conserva una sesiÃ³n segura | A3.1, A2.1 | Payload validado contra vectores, TLS correcto, ticket de un solo uso y credenciales protegidas |
| P4 | CatÃ¡logo local consultable sin red | A2.1 | PaginaciÃ³n completa, rÃ©plica consistente, reinicio/offline correctos y ausencia de datos operativos |
| P5 | Estado personal se edita y sincroniza | A2.2, A5 | Base confirmada por campo; reglas acordadas y cortes de conexiÃ³n probados; borrado contra ediciÃ³n llega a la persona |
| P6 | RecomendaciÃ³n guardada desde texto o Compartir | A2.3, A3.2 y brief cotidiano | Borrador durable e idempotente; identificar no se decide por parecido; revisiÃ³n en servidor |

P2 puede prepararse mientras se prueba P1, porque usa endpoints existentes.
La construcciÃ³n de pantallas Android de P3/P4 empieza al superar el gate inicial
de P1, conservando la decisiÃ³n previa de probar sync antes de construir pantallas.
La matriz completa de A5 sigue siendo requisito para entregar ediciÃ³n P5.
v0.1.0 cierra P3/P4 (M2): aparear y leer. No promete ediciÃ³n/captura antes de sus gates.

## Pruebas sin QR ni telÃ©fono

El arnÃ©s obtiene una sesiÃ³n de dispositivo mediante `POST /api/v1/auth/login`
con una cuenta sintÃ©tica de una instancia descartable. No usa la sesiÃ³n del browser
ni el `/api/` histÃ³rico. Este login es un recurso de prueba, no el onboarding final.
El cliente Kotlin consume la API real; probar sÃ³lo un cliente Python no valida el
cliente Android. Las reglas puras usan vectores y no necesitan emulador.

Primer caso: leer obra/base, cambiar un campo, enviar con precondiciÃ³n, releer
respuesta y verificar base/local confirmadas. DespuÃ©s: dos clientes, ediciÃ³n web
entre lectura/escritura, respuesta perdida, renovaciÃ³n reintentada, paginaciÃ³n
interrumpida, bajas y sesiÃ³n revocada. Conservar los 18 casos de la matriz A5.1.

## Pruebas con el dispositivo

DecisiÃ³n del owner, 2026-10-04: primera prueba en la misma Wi-Fi que el servidor;
el acceso fuera de casa se valida en un incremento posterior.

1. Conectar por USB con depuraciÃ³n autorizada; comprobar `adb devices` e instalar
   el APK debug. Hoy la pantalla existente sÃ³lo comprueba el entorno.
2. Para la primera prueba real, acordar direcciÃ³n alcanzable desde el telÃ©fono
   y TLS. `127.0.0.1` en el telÃ©fono apunta al telÃ©fono; `10.0.2.2` corresponde al
   emulador y su excepciÃ³n HTTP de debug no habilita HTTP en un telÃ©fono real.
3. Escanear, mostrar instancia/cuenta y confirmar; canjear sÃ³lo entonces.
4. Leer toda la rÃ©plica, activar modo aviÃ³n, reiniciar y consultar una ficha.
5. Probar cÃ¡mara denegada, QR invÃ¡lido/vencido/usado, certificado incorrecto,
   caÃ­da durante descarga y revocaciÃ³n desde web. El error explica cÃ³mo retomar.

TLS sigue CLAUDE.md: certificado vÃ¡lido o trust anchor construido desde SPKI del QR,
con verificaciÃ³n de hostname. No adoptar un certificado por un handshake fallido.
Tokens bajo protecciÃ³n de Android Keystore, fuera de logs/URI/backup. El escÃ¡ner
es una pantalla nativa; ZXing puede necesitar una integraciÃ³n con Views.

## Dump web: prueba manual aislada

La rÃ©plica Android se descarga mediante la API; no importarÃ¡ directamente SQLite
de la web. El dump sirve como semilla de una instancia de QA separada.

- Identificar quÃ© se exportÃ³: catÃ¡logo, `instance.db` o exportaciÃ³n portable JSON,
  y versiÃ³n/commit web. `instance.db` por sÃ­ sola no representa todos los catÃ¡logos.
- Usar el mecanismo de backup/exportaciÃ³n del servidor. No copiar a ciegas una
  SQLite abierta: su WAL puede contener cambios que la copia omita.
- Mantener la copia fuera del repo y de fixtures. No usar credenciales reales;
  preferir cuenta de QA y exportaciÃ³n de catÃ¡logo antes que copiar sesiones.
- Separar rutas de datos, puerto y configuraciÃ³n de producciÃ³n. Para el primer
  QA de lectura no ejecutar scanner, enriquecimiento ni sincronizaciÃ³n de escritura.
- Las pruebas automatizadas siguen con datos sintÃ©ticos. El dump privado sÃ³lo
  evalÃºa volumen, contenido y usabilidad durante QA manual autorizado.

No hace falta esperar el dump para P0/P1. El telÃ©fono tampoco bloquea esas tareas.

## Estado comprobado y prÃ³xima tarea

2026-10-04: `assembleDebug --offline` exitoso, APK debug existente; `adb devices`
sin dispositivo conectado. Compose placeholder; no pairing, Room ni cliente sync.
Contrato Android fijado en septiembre y pendiente de actualizaciÃ³n explÃ­cita.
X1â€“X10 del servidor estÃ¡n implementados; X11 estÃ¡ en release/0.11.0. Verificar
el commit elegido al comenzar P0, sin suponer que la release ya se fusionÃ³.
Los endpoints web de QR/sesiones existen; falta su interfaz de usuario.

P0 y primer caso P1 completados el 2026-10-05: contrato fijado en 72954fe,
12 pruebas y APK verdes. Evidencia: `sync-rating-first-cut.md`.
Siguiente incremento: QR/dispositivos web y diseño de conexión/catálogo;
A5.2/A5.3 completas siguen pendientes antes de entregar edición.
La red inicial ya estÃ¡ acordada: misma Wi-Fi. Antes del QA fÃ­sico confirmar
modelo/versiÃ³n Android y forma de ejecuciÃ³n web.
El diseÃ±o visual de pantallas se revisarÃ¡ por separado antes de implementarlo.
