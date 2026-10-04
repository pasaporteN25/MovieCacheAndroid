# Descubrimiento cotidiano y paridad web/Android

Estado: especificación en descubrimiento. Pedido del owner, 2026-10-04.
Este documento fija el objetivo; no declara implementadas las funciones propuestas.

## Objetivo acordado

MovieCacheAndroid será el próximo gran objetivo después del trabajo web en curso.
Web y Android deben servir para buscar películas, directores y actores, descubrir,
guardar recomendaciones y mantener la misma memoria personal mediante sincronización.
El usuario quiere reemplazar, en lo posible, la consulta cotidiana de cine en Google.
Mobile es prioritario para capturar recomendaciones de redes y conversaciones, en
el momento en que aparecen, sin depender de estar en casa o frente a una computadora.

Paridad significa resultados e información coherentes, estado personal compatible y
recorridos útiles en ambas plataformas; no copiar pantallas ni llevar Scanner al teléfono.
El primer apareamiento requiere cuenta web. Después, consulta/edición/captura locales
siguen disponibles offline. El enriquecimiento online y la sincronización se distinguen:
tener red para buscar no obliga a sincronizar la réplica.

## Escenarios que la especificación debe cerrar

1. **Recomendación en persona:** recordar título o persona, encontrar la obra correcta,
   guardar con procedencia/nota si se decide soportarlas y volver a la conversación.
2. **Recomendación de otra app:** recibir texto/enlace mediante Compartir, identificar
   candidatas y guardar sin decidir identidad por parecido. Capturas/OCR están pendientes
   de decisión, no son requisito implícito ni autorizan extracción de redes privadas.
3. **Buscar una persona:** distinguir homónimos, consultar obras y guardar una; concretar
   qué datos y fuentes necesita la búsqueda por actores y qué paridad ofrecerá la web.
4. **Sin conexión:** conservar un borrador sin vencimiento; informar que todavía falta
   identificar/enriquecer, y completarlo cuando haya red sin pisar correcciones.
5. **Volver a casa:** sincronización manual bidireccional, base por campo, reintentos
   seguros, bajas explícitas y conflicto de borrado contra edición visible.

## SDD: cómo llevarlo a entregas

Para cada incremento: escenario → decisiones y dudas → contrato de datos/API → criterios
de aceptación observables → tareas pequeñas → prueba con servidor y teléfono → entrega.
No iniciar una implementación apoyada en una decisión abierta. Los avances independientes
sí pueden seguir mientras el owner responde las preguntas.

Secuencia propuesta, pendiente de confirmar el primer recorrido con el owner:

- **S0 — Alinear contratos:** actualizar `contract/` desde un commit publicado del servidor,
  anotar origen e incorporar vectores de pairing/normalización. Registrar X11 en M2.
- **S1 — Sincronización confiable:** A5.2 (reglas Kotlin) y A5.3 (arnés real); la matriz
  A5.1 ya está cerrada. Revisar textos históricos que aún describen conflictos personales
  manuales: rating máximo, reviews conservadas y vista/fecha reciente son las decisiones.
- **S2 — Primer producto usable:** pairing, almacén local y catálogo offline (M2/v0.1.0).
- **S3 — Recorrido cotidiano elegido:** captura/búsqueda → revisión de identidad → guardado
  offline → enriquecimiento → sync. Cortes web y Android explícitos, no una épica infinita.
- **S4 — Personas y descubrimiento:** contrato de directores/actores, fuentes, cobertura y
  métricas comunes antes de prometer sustitución de Google.

## Preguntas abiertas para el owner

- ¿Primer recorrido: capturar una recomendación, buscar por obra/persona o usar la réplica?
- ¿Entrada habitual: enlace, título/texto, captura o mezcla? ¿Cuáles apps son frecuentes?
- ¿Qué debe responder una ficha para evitar otra búsqueda: sinopsis, reparto, filmografía,
  disponibilidad, puntajes, tráiler, motivo de recomendación u otra información?
- ¿Guardar debe ser una acción rápida y revisar después, o identificar antes de confirmar?
- ¿La procedencia de una recomendación necesita persona, enlace y nota privada?
- ¿Qué significa una búsqueda satisfactoria para títulos incompletos, aliases y homónimos?

## Evidencia actual y dependencias

Android sólo tiene el entorno y la matriz A5.1; no ofrece todavía los escenarios de arriba.
El servidor en `master` ya tiene X1–X10. X11 está implementado en `release/0.11.0`, aún
pendiente de master; los controles web de QR/dispositivos siguen pendientes. `contract/`
del cliente todavía cita el 2026-09-12: actualizarlo como entrega explícita, no editarlo
a mano. PIN/biometría es una decisión aparte y no bloquea el trabajo de reglas puras.

Éxito a validar: captura sin perder la recomendación, resultados con identidad/procedencia
comprensibles y un ciclo offline/sync que conserva todos los cambios. Definir objetivos
de tiempo y corpus con ejemplos del owner; no inventar porcentajes de cobertura.
